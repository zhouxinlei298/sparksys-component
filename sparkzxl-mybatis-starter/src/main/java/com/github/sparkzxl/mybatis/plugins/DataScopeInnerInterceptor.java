package com.github.sparkzxl.mybatis.plugins;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.github.sparkzxl.core.util.ListUtils;
import com.github.sparkzxl.mybatis.constant.SqlConditions;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description: 多列数据权限拦截器
 *
 * @author zhouxinlei
 * @since 2022-07-2022/7/18 13:36:09
 */
@Slf4j
public class DataScopeInnerInterceptor extends JsqlParserSupport implements InnerInterceptor {

    private final static String SELECT = "SELECT";
    private final DataScopeLineHandler dataScopeLineHandler;
    private final DbType dbType;

    public DataScopeInnerInterceptor(DataScopeLineHandler dataScopeLineHandler, DbType dbType) {
        this.dataScopeLineHandler = dataScopeLineHandler;
        this.dbType = dbType;
    }

    /**
     * update 语句处理
     */
    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        final Table table = update.getTable();
        if (dataScopeLineHandler.ignoreTable(table.getName())) {
            // 过滤退出执行
            return;
        }
        if (!SqlCommandType.UPDATE.equals(dataScopeLineHandler.getSqlCommandType())) {
            // 过滤退出执行
            return;
        }
        Expression expression = this.andExpression(table, update.getWhere());
        if (expression == null) {
            return;
        }
        update.setWhere(expression);
        dataScopeLineHandler.remove();
    }

    /**
     * delete 语句处理
     */
    @Override
    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        final Table table = delete.getTable();
        if (dataScopeLineHandler.ignoreTable(delete.getTable().getName())) {
            // 过滤退出执行
            return;
        }
        if (!SqlCommandType.DELETE.equals(dataScopeLineHandler.getSqlCommandType())) {
            // 过滤退出执行
            return;
        }
        Expression expression = this.andExpression(table, delete.getWhere());
        if (expression == null) {
            return;
        }
        delete.setWhere(expression);
        dataScopeLineHandler.remove();
    }

    /**
     * delete update 语句 where 处理
     */
    protected Expression andExpression(Table table, Expression currentExpression) {
        List<String> scopeIdColumnList = dataScopeLineHandler.getScopeIdColumnList();
        List<Expression> expressionList = Lists.newArrayList();
        for (String columnName : scopeIdColumnList) {
            // 返回空即表示当前语句不需要拼接 scopeId
            if (dataScopeLineHandler.getScopeId(columnName) == null) {
                continue;
            }
            //获得where条件表达式
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(this.getAliasColumn(table, columnName));
            equalsTo.setRightExpression(new StringValue(dataScopeLineHandler.getScopeId(columnName)));
            expressionList.add(equalsTo);
        }
        if (currentExpression == null) {
            return new MultiAndExpression(expressionList);
        }
        if (currentExpression instanceof OrExpression) {
            return new AndExpression(new Parenthesis(currentExpression), new MultiAndExpression(expressionList));
        } else {
            return new AndExpression(currentExpression, new MultiAndExpression(expressionList));
        }
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
                            BoundSql boundSql) throws SQLException {
        if (dataScopeLineHandler.match()) {
            PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
            mpBs.sql(parserSingle(mpBs.sql(), null));
        }
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        processSelectBody(select.getSelectBody());
        List<WithItem> withItemsList = select.getWithItemsList();
        if (!CollectionUtils.isEmpty(withItemsList)) {
            withItemsList.forEach(this::processSelectBody);
        }
        dataScopeLineHandler.remove();
    }

    protected void processSelectBody(SelectBody selectBody) {
        if (selectBody == null) {
            return;
        }
        if (selectBody instanceof PlainSelect) {
            processPlainSelect((PlainSelect) selectBody);
        } else if (selectBody instanceof WithItem) {
            WithItem withItem = (WithItem) selectBody;
            processSelectBody(withItem.getSubSelect().getSelectBody());
        } else {
            SetOperationList operationList = (SetOperationList) selectBody;
            List<SelectBody> selectBodyList = operationList.getSelects();
            if (CollectionUtils.isNotEmpty(selectBodyList)) {
                selectBodyList.forEach(this::processSelectBody);
            }
        }
    }

    /**
     * 处理 PlainSelect
     */
    protected void processPlainSelect(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        Expression where = plainSelect.getWhere();
        processWhereSubSelect(where);
        if (fromItem instanceof Table) {
            Table fromTable = (Table) fromItem;
            if (!dataScopeLineHandler.ignoreTable(fromTable.getName())) {
                Expression builderExpression = builderExpression(where, fromTable);
                if (builderExpression != null) {
                    plainSelect.setWhere(builderExpression);
                }
            }
        } else {
            processFromItem(fromItem);
        }
        //#3087 github
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        if (CollectionUtils.isNotEmpty(selectItems)) {
            selectItems.forEach(this::processSelectItem);
        }
        List<Join> joins = plainSelect.getJoins();
        if (CollectionUtils.isNotEmpty(joins)) {
            joins.forEach(j -> {
                processJoin(j);
                processFromItem(j.getRightItem());
            });
        }
    }

    /**
     * 处理where条件内的子查询
     * <p>
     * 支持如下: 1. in 2. = 3. > 4. < 5. >= 6. <= 7. <> 8. EXISTS 9. NOT EXISTS
     * <p>
     * 前提条件: 1. 子查询必须放在小括号中 2. 子查询一般放在比较操作符的右边
     *
     * @param where where 条件
     */
    protected void processWhereSubSelect(Expression where) {
        if (where == null) {
            return;
        }
        if (where instanceof FromItem) {
            processFromItem((FromItem) where);
            return;
        }
        if (where.toString().indexOf(SELECT) > 0) {
            // 有子查询
            if (where instanceof BinaryExpression) {
                // 比较符号 , and , or , 等等
                BinaryExpression expression = (BinaryExpression) where;
                processWhereSubSelect(expression.getLeftExpression());
                processWhereSubSelect(expression.getRightExpression());
            } else if (where instanceof InExpression) {
                // in
                InExpression expression = (InExpression) where;
                ItemsList itemsList = expression.getRightItemsList();
                if (itemsList instanceof SubSelect) {
                    processSelectBody(((SubSelect) itemsList).getSelectBody());
                }
            } else if (where instanceof ExistsExpression) {
                // exists
                ExistsExpression expression = (ExistsExpression) where;
                processWhereSubSelect(expression.getRightExpression());
            } else if (where instanceof NotExpression) {
                // not exists
                NotExpression expression = (NotExpression) where;
                processWhereSubSelect(expression.getExpression());
            } else if (where instanceof Parenthesis) {
                Parenthesis expression = (Parenthesis) where;
                processWhereSubSelect(expression.getExpression());
            }
        }
    }

    protected void processSelectItem(SelectItem selectItem) {
        if (selectItem instanceof SelectExpressionItem) {
            SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
            if (selectExpressionItem.getExpression() instanceof SubSelect) {
                processSelectBody(((SubSelect) selectExpressionItem.getExpression()).getSelectBody());
            } else if (selectExpressionItem.getExpression() instanceof Function) {
                processFunction((Function) selectExpressionItem.getExpression());
            }
        }
    }

    /**
     * 处理函数
     * <p>支持: 1. select fun(args..) 2. select fun1(fun2(args..),args..)<p>
     * <p> fixed gitee pulls/141</p>
     *
     * @param function
     */
    protected void processFunction(Function function) {
        ExpressionList parameters = function.getParameters();
        if (parameters != null) {
            parameters.getExpressions().forEach(expression -> {
                if (expression instanceof SubSelect) {
                    processSelectBody(((SubSelect) expression).getSelectBody());
                } else if (expression instanceof Function) {
                    processFunction((Function) expression);
                }
            });
        }
    }

    /**
     * 处理子查询等
     */
    protected void processFromItem(FromItem fromItem) {
        if (fromItem instanceof SubJoin) {
            SubJoin subJoin = (SubJoin) fromItem;
            if (subJoin.getJoinList() != null) {
                subJoin.getJoinList().forEach(this::processJoin);
            }
            if (subJoin.getLeft() != null) {
                processFromItem(subJoin.getLeft());
            }
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            if (subSelect.getSelectBody() != null) {
                processSelectBody(subSelect.getSelectBody());
            }
        } else if (fromItem instanceof ValuesList) {
            logger.debug("Perform a subquery, if you do not give us feedback");
        } else if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            if (lateralSubSelect.getSubSelect() != null) {
                SubSelect subSelect = lateralSubSelect.getSubSelect();
                if (subSelect.getSelectBody() != null) {
                    processSelectBody(subSelect.getSelectBody());
                }
            }
        }
    }

    /**
     * 处理联接语句
     */
    protected void processJoin(Join join) {
        if (join.getRightItem() instanceof Table) {
            Table fromTable = (Table) join.getRightItem();
            if (dataScopeLineHandler.ignoreTable(fromTable.getName())) {
                // 过滤退出执行
                return;
            }
            Expression builderExpression = builderExpression(join.getOnExpression(), fromTable);
            if (builderExpression == null) {
                return;
            }
            join.setOnExpression(builderExpression);
        }
    }

    /**
     * 处理条件,支持拼接多个scopeId
     *
     * @param currentExpression 表达式
     * @param table             表
     * @return Expression
     */
    protected Expression builderExpression(Expression currentExpression, Table table) {
        List<String> scopeIdColumnList = dataScopeLineHandler.getScopeIdColumnList();
        List<Expression> expressionList = Lists.newArrayList();
        for (String columnName : scopeIdColumnList) {
            SqlConditions sqlCondition = dataScopeLineHandler.getSqlCondition(columnName);
            if (sqlCondition == SqlConditions.EQ) {
                String scopeId = dataScopeLineHandler.getScopeId(columnName);
                EqualsTo equalsTo = new EqualsTo();
                if (scopeId != null) {
                    equalsTo.setLeftExpression(this.getAliasColumn(table, columnName));
                    equalsTo.setRightExpression(new StringValue(scopeId));
                } else {
                    if (dataScopeLineHandler.getForce(columnName)) {
                        IsNullExpression isNullExpression = new IsNullExpression();
                        isNullExpression.setLeftExpression(this.getAliasColumn(table, columnName));
                        expressionList.add(isNullExpression);
                    }
                }
                expressionList.add(equalsTo);
            } else if (sqlCondition == SqlConditions.IN) {
                List<String> scopeIdList = dataScopeLineHandler.getScopeIdList(columnName);
                if (scopeIdList != null) {
                    ValueListExpression valueListExpression = new ValueListExpression();
                    List<Expression> inExpressionList = scopeIdList.stream().map(StringValue::new).collect(Collectors.toList());
                    valueListExpression.setExpressionList(new ExpressionList(inExpressionList));
                    InExpression in = new InExpression();
                    in.setLeftExpression(this.getAliasColumn(table, columnName));
                    in.setRightExpression(valueListExpression);
                    expressionList.add(in);
                } else {
                    if (dataScopeLineHandler.getForce(columnName)) {
                        IsNullExpression isNullExpression = new IsNullExpression();
                        isNullExpression.setLeftExpression(this.getAliasColumn(table, columnName));
                        expressionList.add(isNullExpression);
                    }
                }
            } else if (sqlCondition == SqlConditions.LIST_IN) {
                List<String> scopeIdList = dataScopeLineHandler.getScopeIdList(columnName);
                if (scopeIdList != null) {
                    String functionName = "find_in_set_multiple";
                    log.debug("Check whether the [find_in_set_multiple] function is available in {}", dbType.getDb());
                    Function function = new Function();
                    // 设置函数名
                    function.setName(functionName);
                    // 创建参数表达式
                    ExpressionList expressionListCount = new ExpressionList();
                    expressionListCount.setExpressions(
                            Lists.newArrayList(new StringValue(ListUtils.listToString(scopeIdList)), this.getAliasColumn(table, columnName)));
                    // 设置参数
                    function.setParameters(expressionListCount);
                    expressionList.add(function);
                } else {
                    if (dataScopeLineHandler.getForce(columnName)) {
                        IsNullExpression isNullExpression = new IsNullExpression();
                        isNullExpression.setLeftExpression(this.getAliasColumn(table, columnName));
                        expressionList.add(isNullExpression);
                    }
                }
            } else if (sqlCondition == SqlConditions.LIKE) {
                String scopeId = dataScopeLineHandler.getScopeId(columnName);
                if (scopeId != null) {
                    LikeExpression likeExpression = new LikeExpression();
                    likeExpression.setLeftExpression(this.getAliasColumn(table, columnName));
                    likeExpression.setRightExpression(new StringValue("%".concat(scopeId).concat("%")));
                    expressionList.add(likeExpression);
                } else {
                    if (dataScopeLineHandler.getForce(columnName)) {
                        IsNullExpression isNullExpression = new IsNullExpression();
                        isNullExpression.setLeftExpression(this.getAliasColumn(table, columnName));
                        expressionList.add(isNullExpression);
                    }
                }
            }
        }
        if (currentExpression == null) {
            return new MultiAndExpression(expressionList);
        }
        if (currentExpression instanceof OrExpression) {
            return new AndExpression(new Parenthesis(currentExpression), new MultiAndExpression(expressionList));
        } else {
            return new AndExpression(currentExpression, new MultiAndExpression(expressionList));
        }
    }

    /**
     * 数据权限字段别名设置
     * <p>scopeId 或 tableAlias.scopeId</p>
     *
     * @param table      表对象
     * @param columnName 字段名
     * @return 字段
     */
    protected Column getAliasColumn(Table table, String columnName) {
        StringBuilder column = new StringBuilder();
        if (table.getAlias() != null) {
            column.append(table.getAlias().getName()).append(StringPool.DOT);
        }
        column.append(columnName);
        return new Column(column.toString());
    }
}
