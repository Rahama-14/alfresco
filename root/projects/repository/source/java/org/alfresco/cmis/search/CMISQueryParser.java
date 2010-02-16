/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.cmis.search;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.alfresco.cmis.CMISCardinalityEnum;
import org.alfresco.cmis.CMISDictionaryService;
import org.alfresco.cmis.CMISJoinEnum;
import org.alfresco.cmis.CMISPropertyDefinition;
import org.alfresco.cmis.CMISQueryException;
import org.alfresco.cmis.CMISQueryOptions;
import org.alfresco.cmis.CMISScope;
import org.alfresco.cmis.CMISTypeDefinition;
import org.alfresco.cmis.CMISQueryOptions.CMISQueryMode;
import org.alfresco.repo.search.impl.parsers.CMISLexer;
import org.alfresco.repo.search.impl.parsers.CMISParser;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.ArgumentDefinition;
import org.alfresco.repo.search.impl.querymodel.Column;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.Function;
import org.alfresco.repo.search.impl.querymodel.FunctionArgument;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.JoinType;
import org.alfresco.repo.search.impl.querymodel.ListArgument;
import org.alfresco.repo.search.impl.querymodel.LiteralArgument;
import org.alfresco.repo.search.impl.querymodel.Order;
import org.alfresco.repo.search.impl.querymodel.Ordering;
import org.alfresco.repo.search.impl.querymodel.ParameterArgument;
import org.alfresco.repo.search.impl.querymodel.PropertyArgument;
import org.alfresco.repo.search.impl.querymodel.Query;
import org.alfresco.repo.search.impl.querymodel.QueryModelFactory;
import org.alfresco.repo.search.impl.querymodel.Selector;
import org.alfresco.repo.search.impl.querymodel.SelectorArgument;
import org.alfresco.repo.search.impl.querymodel.Source;
import org.alfresco.repo.search.impl.querymodel.Constraint.Occur;
import org.alfresco.repo.search.impl.querymodel.QueryOptions.Connective;
import org.alfresco.repo.search.impl.querymodel.impl.BaseComparison;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Child;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Descendant;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Equals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Exists;
import org.alfresco.repo.search.impl.querymodel.impl.functions.GreaterThan;
import org.alfresco.repo.search.impl.querymodel.impl.functions.GreaterThanOrEquals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.In;
import org.alfresco.repo.search.impl.querymodel.impl.functions.LessThan;
import org.alfresco.repo.search.impl.querymodel.impl.functions.LessThanOrEquals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Like;
import org.alfresco.repo.search.impl.querymodel.impl.functions.NotEquals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.PropertyAccessor;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Score;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.surf.util.CachingDateFormat;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

/**
 * @author andyh
 */
public class CMISQueryParser
{
    private CMISQueryOptions options;

    private CMISDictionaryService cmisDictionaryService;

    private CMISJoinEnum joinSupport;

    private CMISScope[] validScopes;

    private static CMISScope[] STRICT_SCOPES = new CMISScope[] { CMISScope.DOCUMENT, CMISScope.FOLDER };

    private static CMISScope[] ALFRESCO_SCOPES = new CMISScope[] { CMISScope.DOCUMENT, CMISScope.FOLDER, CMISScope.POLICY };

    public CMISQueryParser(CMISQueryOptions options, CMISDictionaryService cmisDictionaryService, CMISJoinEnum joinSupport)
    {
        this.options = options;
        this.cmisDictionaryService = cmisDictionaryService;
        this.joinSupport = joinSupport;
        this.validScopes = (options.getQueryMode() == CMISQueryMode.CMS_STRICT) ? STRICT_SCOPES : ALFRESCO_SCOPES;
    }

    public Query parse(QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext)
    {
        CMISParser parser = null;
        try
        {
            CharStream cs = new ANTLRStringStream(options.getQuery());
            CMISLexer lexer = new CMISLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            parser = new CMISParser(tokens);
            parser.setStrict(options.getQueryMode() == CMISQueryMode.CMS_STRICT);
            CommonTree queryNode = (CommonTree) parser.query().getTree();

            CommonTree sourceNode = (CommonTree) queryNode.getFirstChildWithType(CMISParser.SOURCE);
            Source source = buildSource(sourceNode, joinSupport, factory);
            Map<String, Selector> selectors = source.getSelectors();
            ArrayList<Column> columns = buildColumns(queryNode, factory, selectors, options.getQuery());

            HashSet<String> columnNames = new HashSet<String>();
            for (Column column : columns)
            {
                if (!columnNames.add(column.getAlias()))
                {
                    throw new CMISQueryException("Duplicate column alias for " + column.getAlias());
                }
            }

            ArrayList<Ordering> orderings = buildOrderings(queryNode, factory, selectors, columns);

            Constraint constraint = null;
            CommonTree orNode = (CommonTree) queryNode.getFirstChildWithType(CMISParser.DISJUNCTION);
            if (orNode != null)
            {
                constraint = buildDisjunction(orNode, factory, functionEvaluationContext, selectors, columns);
            }

            Query query = factory.createQuery(columns, source, constraint, orderings);

            // TODO: validate query and use of ID, function arguments matching
            // up etc

            return query;
        }
        catch (RecognitionException e)
        {
            if (parser != null)
            {
                String[] tokenNames = parser.getTokenNames();
                String hdr = parser.getErrorHeader(e);
                String msg = parser.getErrorMessage(e, tokenNames);
                throw new CMISQueryException(hdr + "\n" + msg, e);
            }
        }
        throw new CMISQueryException("Failed to parse");
    }

    /**
     * @param queryNode
     * @param factory
     * @param selectors
     * @param columns
     * @return
     */
    private Constraint buildDisjunction(CommonTree orNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Map<String, Selector> selectors,
            ArrayList<Column> columns)
    {
        List<Constraint> constraints = new ArrayList<Constraint>(orNode.getChildCount());
        for (int i = 0; i < orNode.getChildCount(); i++)
        {
            CommonTree andNode = (CommonTree) orNode.getChild(i);
            Constraint constraint = buildConjunction(andNode, factory, functionEvaluationContext, selectors, columns);
            constraints.add(constraint);
        }
        if (constraints.size() == 1)
        {
            return constraints.get(0);
        }
        else
        {
            return factory.createDisjunction(constraints);
        }
    }

    /**
     * @param andNode
     * @param factory
     * @param selectors
     * @param columns
     * @return
     */
    private Constraint buildConjunction(CommonTree andNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Map<String, Selector> selectors,
            ArrayList<Column> columns)
    {
        List<Constraint> constraints = new ArrayList<Constraint>(andNode.getChildCount());
        for (int i = 0; i < andNode.getChildCount(); i++)
        {
            CommonTree notNode = (CommonTree) andNode.getChild(i);
            Constraint constraint = buildNegation(notNode, factory, functionEvaluationContext, selectors, columns);
            constraints.add(constraint);
        }
        if (constraints.size() == 1)
        {
            return constraints.get(0);
        }
        else
        {
            return factory.createConjunction(constraints);
        }
    }

    /**
     * @param notNode
     * @param factory
     * @param selectors
     * @param columns
     * @return
     */
    private Constraint buildNegation(CommonTree notNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Map<String, Selector> selectors,
            ArrayList<Column> columns)
    {
        if (notNode.getType() == CMISParser.NEGATION)
        {
            Constraint constraint = buildTest((CommonTree) notNode.getChild(0), factory, functionEvaluationContext, selectors, columns);
            constraint.setOccur(Occur.EXCLUDE);
            return constraint;
        }
        else
        {
            return buildTest(notNode, factory, functionEvaluationContext, selectors, columns);
        }
    }

    /**
     * @param notNode
     * @param factory
     * @param selectors
     * @param columns
     * @return
     */
    private Constraint buildTest(CommonTree testNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Map<String, Selector> selectors,
            ArrayList<Column> columns)
    {
        if (testNode.getType() == CMISParser.DISJUNCTION)
        {
            return buildDisjunction(testNode, factory, functionEvaluationContext, selectors, columns);
        }
        else
        {
            return buildPredicate(testNode, factory, functionEvaluationContext, selectors, columns);
        }
    }

    /**
     * @param orNode
     * @param factory
     * @param selectors
     * @param columns
     * @return
     */
    private Constraint buildPredicate(CommonTree predicateNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Map<String, Selector> selectors,
            ArrayList<Column> columns)
    {
        String functionName;
        Function function;
        CommonTree argNode;
        Map<String, Argument> functionArguments;
        Argument arg;
        switch (predicateNode.getType())
        {
        case CMISParser.PRED_CHILD:
            functionName = Child.NAME;
            function = factory.getFunction(functionName);
            functionArguments = new LinkedHashMap<String, Argument>();
            argNode = (CommonTree) predicateNode.getChild(0);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(Child.ARG_PARENT), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            if (predicateNode.getChildCount() > 1)
            {
                arg = getFunctionArgument(argNode, function.getArgumentDefinition(Child.ARG_SELECTOR), factory, selectors);
                if (!arg.isQueryable())
                {
                    throw new CMISQueryException("The property is not queryable: " + argNode.getText());
                }
                functionArguments.put(arg.getName(), arg);
            }
            return factory.createFunctionalConstraint(function, functionArguments);
        case CMISParser.PRED_COMPARISON:
            switch (predicateNode.getChild(2).getType())
            {
            case CMISParser.EQUALS:
                functionName = Equals.NAME;
                function = factory.getFunction(functionName);
                break;
            case CMISParser.NOTEQUALS:
                functionName = NotEquals.NAME;
                function = factory.getFunction(functionName);
                break;
            case CMISParser.GREATERTHAN:
                functionName = GreaterThan.NAME;
                function = factory.getFunction(functionName);
                break;
            case CMISParser.GREATERTHANOREQUALS:
                functionName = GreaterThanOrEquals.NAME;
                function = factory.getFunction(functionName);
                break;
            case CMISParser.LESSTHAN:
                functionName = LessThan.NAME;
                function = factory.getFunction(functionName);
                break;
            case CMISParser.LESSTHANOREQUALS:
                functionName = LessThanOrEquals.NAME;
                function = factory.getFunction(functionName);
                break;
            default:
                throw new CMISQueryException("Unknown comparison function " + predicateNode.getChild(2).getText());
            }
            functionArguments = new LinkedHashMap<String, Argument>();
            argNode = (CommonTree) predicateNode.getChild(1);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(BaseComparison.ARG_LHS), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            argNode = (CommonTree) predicateNode.getChild(3);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(BaseComparison.ARG_RHS), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            return factory.createFunctionalConstraint(function, functionArguments);
        case CMISParser.PRED_DESCENDANT:
            functionName = Descendant.NAME;
            function = factory.getFunction(functionName);
            argNode = (CommonTree) predicateNode.getChild(0);
            functionArguments = new LinkedHashMap<String, Argument>();
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(Descendant.ARG_ANCESTOR), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            if (predicateNode.getChildCount() > 1)
            {
                arg = getFunctionArgument(argNode, function.getArgumentDefinition(Descendant.ARG_SELECTOR), factory, selectors);
                functionArguments.put(arg.getName(), arg);
            }
            return factory.createFunctionalConstraint(function, functionArguments);
        case CMISParser.PRED_EXISTS:
            functionName = Exists.NAME;
            function = factory.getFunction(functionName);
            argNode = (CommonTree) predicateNode.getChild(0);
            functionArguments = new LinkedHashMap<String, Argument>();
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(Exists.ARG_PROPERTY), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            arg = factory.createLiteralArgument(Exists.ARG_NOT, DataTypeDefinition.BOOLEAN, (predicateNode.getChildCount() > 1));
            functionArguments.put(arg.getName(), arg);
            return factory.createFunctionalConstraint(function, functionArguments);
        case CMISParser.PRED_FTS:
            String ftsExpression = predicateNode.getChild(0).getText();
            FTSQueryParser ftsQueryParser = new FTSQueryParser();
            Selector selector;
            if (predicateNode.getChildCount() > 1)
            {
                String qualifier = predicateNode.getChild(1).getText();
                selector = selectors.get(qualifier);
                if (selector == null)
                {
                    throw new CMISQueryException("No selector for " + qualifier);
                }
            }
            else
            {
                if (selectors.size() == 1)
                {
                    selector = selectors.get(selectors.keySet().iterator().next());
                }
                else
                {
                    throw new CMISQueryException("A selector must be specified when there are two or more selectors");
                }
            }
            Connective defaultConnective;
            Connective defaultFieldConnective;
            if (options.getQueryMode() == CMISQueryMode.CMS_STRICT)
            {
                defaultConnective = Connective.AND;
                defaultFieldConnective = Connective.AND;
            }
            else
            {
                defaultConnective = options.getDefaultFTSConnective();
                defaultFieldConnective = options.getDefaultFTSFieldConnective();
            }
            return ftsQueryParser.buildFTS(ftsExpression.substring(1, ftsExpression.length() - 1), factory, functionEvaluationContext, selector, columns, defaultConnective,
                    defaultFieldConnective, null, options.getDefaultFieldName());
        case CMISParser.PRED_IN:
            functionName = In.NAME;
            function = factory.getFunction(functionName);
            functionArguments = new LinkedHashMap<String, Argument>();
            argNode = (CommonTree) predicateNode.getChild(0);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(In.ARG_MODE), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            argNode = (CommonTree) predicateNode.getChild(1);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(In.ARG_PROPERTY), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            argNode = (CommonTree) predicateNode.getChild(2);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(In.ARG_LIST), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            arg = factory.createLiteralArgument(In.ARG_NOT, DataTypeDefinition.BOOLEAN, (predicateNode.getChildCount() > 3));
            functionArguments.put(arg.getName(), arg);
            return factory.createFunctionalConstraint(function, functionArguments);
        case CMISParser.PRED_LIKE:
            functionName = Like.NAME;
            function = factory.getFunction(functionName);
            functionArguments = new LinkedHashMap<String, Argument>();
            argNode = (CommonTree) predicateNode.getChild(0);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(Like.ARG_PROPERTY), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            argNode = (CommonTree) predicateNode.getChild(1);
            arg = getFunctionArgument(argNode, function.getArgumentDefinition(Like.ARG_EXP), factory, selectors);
            functionArguments.put(arg.getName(), arg);
            arg = factory.createLiteralArgument(Like.ARG_NOT, DataTypeDefinition.BOOLEAN, (predicateNode.getChildCount() > 2));
            functionArguments.put(arg.getName(), arg);
            return factory.createFunctionalConstraint(function, functionArguments);
        default:
            return null;
        }
    }

    /**
     * @param queryNode
     * @param factory
     * @param selectors
     * @return
     */
    private ArrayList<Ordering> buildOrderings(CommonTree queryNode, QueryModelFactory factory, Map<String, Selector> selectors, List<Column> columns)
    {
        ArrayList<Ordering> orderings = new ArrayList<Ordering>();
        CommonTree orderNode = (CommonTree) queryNode.getFirstChildWithType(CMISParser.ORDER);
        if (orderNode != null)
        {
            for (int i = 0; i < orderNode.getChildCount(); i++)
            {
                CommonTree current = (CommonTree) orderNode.getChild(i);

                CommonTree columnRefNode = (CommonTree) current.getFirstChildWithType(CMISParser.COLUMN_REF);
                if (columnRefNode != null)
                {
                    String columnName = columnRefNode.getChild(0).getText();
                    String qualifier = "";
                    if (columnRefNode.getChildCount() > 1)
                    {
                        qualifier = columnRefNode.getChild(1).getText();
                    }

                    Order order = Order.ASCENDING;

                    if (current.getChild(1).getType() == CMISParser.DESC)
                    {
                        order = Order.DESCENDING;
                    }

                    Column orderColumn = null;

                    if (qualifier.length() == 0)
                    {
                        Column match = null;
                        for (Column column : columns)
                        {
                            if (column.getAlias().equals(columnName))
                            {
                                match = column;
                                break;
                            }
                        }
                        // in strict mode the ordered column must be selected
                        if ((options.getQueryMode() == CMISQueryMode.CMS_STRICT) && (match == null))
                        {
                            throw new CMISQueryException("Ordered column is not selected: " + qualifier + "." + columnName);
                        }
                        if (match == null)
                        {

                            Selector selector = selectors.get(qualifier);
                            if (selector == null)
                            {
                                if ((qualifier.equals("")) && (selectors.size() == 1))
                                {
                                    selector = selectors.get(selectors.keySet().iterator().next());
                                }
                                else
                                {
                                    throw new CMISQueryException("No selector for " + qualifier);
                                }
                            }

                            CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), CMISScope.DOCUMENT, CMISScope.FOLDER);
                            if (typeDef == null)
                            {
                                throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                            }
                            CMISPropertyDefinition propDef = cmisDictionaryService.findPropertyByQueryName(columnName);
                            if (propDef == null)
                            {
                                throw new CMISQueryException("Invalid column for " + typeDef.getQueryName() + "." + columnName);
                            }

                            // check there is a matching selector

                            if (options.getQueryMode() == CMISQueryMode.CMS_STRICT)
                            {
                                boolean found = false;
                                for (Column column : columns)
                                {
                                    if (column.getFunction().getName().equals(PropertyAccessor.NAME))
                                    {
                                        PropertyArgument pa = (PropertyArgument) column.getFunctionArguments().get(PropertyAccessor.ARG_PROPERTY);
                                        if (pa.getPropertyName().equals(propDef.getPropertyId().getId()))
                                        {
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (!found)
                                {
                                    throw new CMISQueryException("Ordered column is not selected: " + qualifier + "." + columnName);
                                }
                            }

                            Function function = factory.getFunction(PropertyAccessor.NAME);
                            Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, propDef.isQueryable(), propDef.isOrderable(), selector.getAlias(), propDef
                                    .getPropertyId().getId());
                            Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                            functionArguments.put(arg.getName(), arg);

                            String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + propDef.getPropertyId().getId() : propDef.getPropertyId().getId();

                            match = factory.createColumn(function, functionArguments, alias);
                        }

                        orderColumn = match;
                    }
                    else
                    {
                        Selector selector = selectors.get(qualifier);
                        if (selector == null)
                        {
                            if ((qualifier.equals("")) && (selectors.size() == 1))
                            {
                                selector = selectors.get(selectors.keySet().iterator().next());
                            }
                            else
                            {
                                throw new CMISQueryException("No selector for " + qualifier);
                            }
                        }

                        CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), CMISScope.DOCUMENT, CMISScope.FOLDER);
                        if (typeDef == null)
                        {
                            throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                        }
                        CMISPropertyDefinition propDef = cmisDictionaryService.findPropertyByQueryName(columnName);
                        if (propDef == null)
                        {
                            throw new CMISQueryException("Invalid column for " + typeDef.getQueryName() + "." + columnName + " selector alias " + selector.getAlias());
                        }

                        // check there is a matching selector

                        if (options.getQueryMode() == CMISQueryMode.CMS_STRICT)
                        {
                            boolean found = false;
                            for (Column column : columns)
                            {
                                if (column.getFunction().getName().equals(PropertyAccessor.NAME))
                                {
                                    PropertyArgument pa = (PropertyArgument) column.getFunctionArguments().get(PropertyAccessor.ARG_PROPERTY);
                                    if (pa.getPropertyName().equals(propDef.getPropertyId().getId()))
                                    {
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (!found)
                            {
                                throw new CMISQueryException("Ordered column is not selected: " + qualifier + "." + columnName);
                            }
                        }

                        Function function = factory.getFunction(PropertyAccessor.NAME);
                        Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, propDef.isQueryable(), propDef.isOrderable(), selector.getAlias(), propDef
                                .getPropertyId().getId());
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                        functionArguments.put(arg.getName(), arg);

                        String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + propDef.getPropertyId().getId() : propDef.getPropertyId().getId();

                        orderColumn = factory.createColumn(function, functionArguments, alias);
                    }

                    if (!orderColumn.isOrderable() || !orderColumn.isQueryable())
                    {
                        throw new CMISQueryException("Ordering is not support for " + orderColumn.getAlias());
                    }

                    Ordering ordering = factory.createOrdering(orderColumn, order);
                    orderings.add(ordering);

                }
            }
        }
        return orderings;
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Column> buildColumns(CommonTree queryNode, QueryModelFactory factory, Map<String, Selector> selectors, String query)
    {
        ArrayList<Column> columns = new ArrayList<Column>();
        CommonTree starNode = (CommonTree) queryNode.getFirstChildWithType(CMISParser.ALL_COLUMNS);
        if (starNode != null)
        {
            for (Selector selector : selectors.values())
            {
                CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), validScopes);
                if (typeDef == null)
                {
                    throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                }
                Map<String, CMISPropertyDefinition> propDefs = typeDef.getPropertyDefinitions();
                for (CMISPropertyDefinition definition : propDefs.values())
                {
                    if (definition.getCardinality() == CMISCardinalityEnum.SINGLE_VALUED)
                    {
                        Function function = factory.getFunction(PropertyAccessor.NAME);
                        Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, definition.isQueryable(), definition.isOrderable(), selector.getAlias(),
                                definition.getPropertyId().getId());
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                        functionArguments.put(arg.getName(), arg);
                        String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + definition.getPropertyId().getId() : definition.getPropertyId().getId();
                        Column column = factory.createColumn(function, functionArguments, alias);
                        if (column.isQueryable())
                        {
                            columns.add(column);
                        }
                    }
                }
            }
        }

        CommonTree columnsNode = (CommonTree) queryNode.getFirstChildWithType(CMISParser.COLUMNS);
        if (columnsNode != null)
        {
            for (CommonTree columnNode : (List<CommonTree>) columnsNode.getChildren())
            {
                if (columnNode.getType() == CMISParser.ALL_COLUMNS)
                {
                    String qualifier = columnNode.getChild(0).getText();
                    Selector selector = selectors.get(qualifier);
                    if (selector == null)
                    {
                        if ((qualifier.equals("")) && (selectors.size() == 1))
                        {
                            selector = selectors.get(selectors.keySet().iterator().next());
                        }
                        else
                        {
                            throw new CMISQueryException("No selector for " + qualifier + " in " + qualifier + ".*");
                        }
                    }

                    CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), validScopes);
                    if (typeDef == null)
                    {
                        throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                    }
                    Map<String, CMISPropertyDefinition> propDefs = typeDef.getPropertyDefinitions();
                    for (CMISPropertyDefinition definition : propDefs.values())
                    {
                        if (definition.getCardinality() == CMISCardinalityEnum.SINGLE_VALUED)
                        {
                            Function function = factory.getFunction(PropertyAccessor.NAME);
                            Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, definition.isQueryable(), definition.isOrderable(), selector.getAlias(),
                                    definition.getPropertyId().getId());
                            Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                            functionArguments.put(arg.getName(), arg);
                            String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + definition.getPropertyId().getId() : definition.getPropertyId().getId();
                            Column column = factory.createColumn(function, functionArguments, alias);
                            if (column.isQueryable())
                            {
                                columns.add(column);
                            }
                        }
                    }
                }

                if (columnNode.getType() == CMISParser.COLUMN)
                {
                    CommonTree columnRefNode = (CommonTree) columnNode.getFirstChildWithType(CMISParser.COLUMN_REF);
                    if (columnRefNode != null)
                    {
                        String columnName = columnRefNode.getChild(0).getText();
                        String qualifier = "";
                        if (columnRefNode.getChildCount() > 1)
                        {
                            qualifier = columnRefNode.getChild(1).getText();
                        }
                        Selector selector = selectors.get(qualifier);
                        if (selector == null)
                        {
                            if ((qualifier.equals("")) && (selectors.size() == 1))
                            {
                                selector = selectors.get(selectors.keySet().iterator().next());
                            }
                            else
                            {
                                throw new CMISQueryException("No selector for " + qualifier);
                            }
                        }

                        CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), validScopes);
                        if (typeDef == null)
                        {
                            throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                        }
                        CMISPropertyDefinition propDef = cmisDictionaryService.findPropertyByQueryName(columnName);
                        if (propDef == null)
                        {
                            throw new CMISQueryException("Invalid column for " + typeDef.getQueryName() + "." + columnName);
                        }

                        Function function = factory.getFunction(PropertyAccessor.NAME);
                        Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, propDef.isQueryable(), propDef.isOrderable(), selector.getAlias(), propDef
                                .getPropertyId().getId());
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                        functionArguments.put(arg.getName(), arg);

                        String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + propDef.getPropertyId().getId() : propDef.getPropertyId().getId();
                        if (columnNode.getChildCount() > 1)
                        {
                            alias = columnNode.getChild(1).getText();
                        }

                        Column column = factory.createColumn(function, functionArguments, alias);

                        if (!column.isQueryable())
                        {
                            throw new CMISQueryException("Column is not queryable " + typeDef.getQueryName() + "." + columnName);
                        }

                        columns.add(column);

                    }

                    CommonTree functionNode = (CommonTree) columnNode.getFirstChildWithType(CMISParser.FUNCTION);
                    if (functionNode != null)
                    {
                        CommonTree functionNameNode = (CommonTree) functionNode.getChild(0);
                        Function function = factory.getFunction(functionNameNode.getText());
                        if (function == null)
                        {
                            throw new CMISQueryException("Unknown function: " + functionNameNode.getText());
                        }
                        Collection<ArgumentDefinition> definitions = function.getArgumentDefinitions().values();
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();

                        int childIndex = 2;
                        for (ArgumentDefinition definition : definitions)
                        {
                            if (functionNode.getChildCount() > childIndex + 1)
                            {
                                CommonTree argNode = (CommonTree) functionNode.getChild(childIndex++);
                                Argument arg = getFunctionArgument(argNode, definition, factory, selectors);
                                functionArguments.put(arg.getName(), arg);
                            }
                            else
                            {
                                if (definition.isMandatory())
                                {
                                    // throw new
                                    // CMISQueryException("Insufficient aruments
                                    // for function " +
                                    // ((CommonTree)
                                    // functionNode.getChild(0)).getText() );
                                    break;
                                }
                                else
                                {
                                    // ok
                                }
                            }
                        }

                        CommonTree rparenNode = (CommonTree) functionNode.getChild(functionNode.getChildCount() - 1);

                        int start = getStringPosition(query, functionNode.getLine(), functionNode.getCharPositionInLine());
                        int end = getStringPosition(query, rparenNode.getLine(), rparenNode.getCharPositionInLine());

                        String alias;
                        if (function.getName().equals(Score.NAME))
                        {
                            alias = "SEARCH_SCORE";
                            // check no args
                            if (functionNode.getChildCount() > 3)
                            {
                                throw new CMISQueryException("The function SCORE() is not allowed any arguments");
                            }
                        }
                        else
                        {
                            alias = query.substring(start, end + 1);
                        }
                        if (columnNode.getChildCount() > 1)
                        {
                            alias = columnNode.getChild(1).getText();
                        }

                        Column column = factory.createColumn(function, functionArguments, alias);
                        columns.add(column);
                    }
                }
            }
        }

        return columns;
    }

    /**
     * @param query
     * @param line
     * @param charPositionInLine
     * @return
     */
    private int getStringPosition(String query, int line, int charPositionInLine)
    {
        StringTokenizer tokenizer = new StringTokenizer(query, "\n\r\f");
        String[] lines = new String[tokenizer.countTokens()];
        int i = 0;
        while (tokenizer.hasMoreElements())
        {
            lines[i++] = tokenizer.nextToken();
        }

        int position = 0;
        for (i = 0; i < line - 1; i++)
        {
            position += lines[i].length();
            position++;
        }
        return position + charPositionInLine;
    }

    private Argument getFunctionArgument(CommonTree argNode, ArgumentDefinition definition, QueryModelFactory factory, Map<String, Selector> selectors)
    {
        if (argNode.getType() == CMISParser.COLUMN_REF)
        {
            PropertyArgument arg = buildColumnReference(definition.getName(), argNode, factory);
            if (!arg.isQueryable())
            {
                throw new CMISQueryException("Column refers to unqueryable property " + arg.getPropertyName());
            }
            if (!selectors.containsKey(arg.getSelector()))
            {
                throw new CMISQueryException("No table with alias " + arg.getSelector());
            }
            return arg;
        }
        else if (argNode.getType() == CMISParser.ID)
        {
            String id = argNode.getText();
            if (selectors.containsKey(id))
            {
                SelectorArgument arg = factory.createSelectorArgument(definition.getName(), id);
                if (!arg.isQueryable())
                {
                    throw new CMISQueryException("Selector is not queryable " + arg.getSelector());
                }
                return arg;
            }
            else
            {
                CMISPropertyDefinition propDef = cmisDictionaryService.findPropertyByQueryName(id);
                if (propDef == null || !propDef.isQueryable())
                {
                    throw new CMISQueryException("Column refers to unqueryable property " + definition.getName());
                }
                PropertyArgument arg = factory.createPropertyArgument(definition.getName(), propDef.isQueryable(), propDef.isOrderable(), "", propDef.getPropertyId().getId());
                return arg;
            }
        }
        else if (argNode.getType() == CMISParser.PARAMETER)
        {
            ParameterArgument arg = factory.createParameterArgument(definition.getName(), argNode.getText());
            if (!arg.isQueryable())
            {
                throw new CMISQueryException("Parameter is not queryable " + arg.getParameterName());
            }
            return arg;
        }
        else if (argNode.getType() == CMISParser.NUMERIC_LITERAL)
        {
            CommonTree literalNode = (CommonTree) argNode.getChild(0);
            if (literalNode.getType() == CMISParser.FLOATING_POINT_LITERAL)
            {
                QName type = DataTypeDefinition.DOUBLE;
                Number value = Double.parseDouble(literalNode.getText());
                if (value.floatValue() == value.doubleValue())
                {
                    type = DataTypeDefinition.FLOAT;
                    value = Float.valueOf(value.floatValue());
                }
                LiteralArgument arg = factory.createLiteralArgument(definition.getName(), type, value);
                return arg;
            }
            else if (literalNode.getType() == CMISParser.DECIMAL_INTEGER_LITERAL)
            {
                QName type = DataTypeDefinition.LONG;
                Number value = Long.parseLong(literalNode.getText());
                if (value.intValue() == value.longValue())
                {
                    type = DataTypeDefinition.INT;
                    value = Integer.valueOf(value.intValue());
                }
                LiteralArgument arg = factory.createLiteralArgument(definition.getName(), type, value);
                return arg;
            }
            else
            {
                throw new CMISQueryException("Invalid numeric literal " + literalNode.getText());
            }
        }
        else if (argNode.getType() == CMISParser.STRING_LITERAL)
        {
            String text = argNode.getChild(0).getText();
            text = text.substring(1, text.length() - 1);
            LiteralArgument arg = factory.createLiteralArgument(definition.getName(), DataTypeDefinition.TEXT, text);
            return arg;
        }
        else if (argNode.getType() == CMISParser.DATETIME_LITERAL)
        {
            String text = argNode.getChild(0).getText();
            text = text.substring(1, text.length() - 1);
            StringBuilder builder = new StringBuilder();
            if (text.endsWith("Z"))
            {
                builder.append(text.substring(0, text.length() - 1));
                builder.append("+0000");
            }
            else
            {
                if (text.charAt(text.length() - 3) != ':')
                {
                    throw new CMISQueryException("Invalid datetime literal " + text);
                }
                // remove TZ colon ....
                builder.append(text.substring(0, text.length() - 3));
                builder.append(text.substring(text.length() - 2, text.length()));
            }
            text = builder.toString();

            SimpleDateFormat df = CachingDateFormat.getCmisSqlDatetimeFormat();
            Date date;
            try
            {
                date = df.parse(text);
            }
            catch (ParseException e)
            {
                throw new CMISQueryException("Invalid datetime literal " + text);
            }
            // Convert back :-)
            String alfrescoDate = DefaultTypeConverter.INSTANCE.convert(String.class, date);
            LiteralArgument arg = factory.createLiteralArgument(definition.getName(), DataTypeDefinition.TEXT, alfrescoDate);
            return arg;
        }
        else if (argNode.getType() == CMISParser.BOOLEAN_LITERAL)
        {
            String text = argNode.getChild(0).getText();
            if (text.equalsIgnoreCase("TRUE") || text.equalsIgnoreCase("FALSE"))
            {
                LiteralArgument arg = factory.createLiteralArgument(definition.getName(), DataTypeDefinition.TEXT, text);
                return arg;
            }
            else
            {
                throw new CMISQueryException("Invalid boolean literal " + text);
            }

        }
        else if (argNode.getType() == CMISParser.LIST)
        {
            ArrayList<Argument> arguments = new ArrayList<Argument>();
            for (int i = 0; i < argNode.getChildCount(); i++)
            {
                CommonTree arg = (CommonTree) argNode.getChild(i);
                arguments.add(getFunctionArgument(arg, definition, factory, selectors));
            }
            ListArgument arg = factory.createListArgument(definition.getName(), arguments);
            if (!arg.isQueryable())
            {
                throw new CMISQueryException("Not all members of the list are queryable");
            }
            return arg;
        }
        else if (argNode.getType() == CMISParser.ANY)
        {
            LiteralArgument arg = factory.createLiteralArgument(definition.getName(), DataTypeDefinition.TEXT, argNode.getText());
            return arg;
        }
        else if (argNode.getType() == CMISParser.NOT)
        {
            LiteralArgument arg = factory.createLiteralArgument(definition.getName(), DataTypeDefinition.TEXT, argNode.getText());
            return arg;
        }
        else if (argNode.getType() == CMISParser.FUNCTION)
        {
            CommonTree functionNameNode = (CommonTree) argNode.getChild(0);
            Function function = factory.getFunction(functionNameNode.getText());
            if (function == null)
            {
                throw new CMISQueryException("Unknown function: " + functionNameNode.getText());
            }
            Collection<ArgumentDefinition> definitions = function.getArgumentDefinitions().values();
            Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();

            int childIndex = 2;
            for (ArgumentDefinition currentDefinition : definitions)
            {
                if (argNode.getChildCount() > childIndex + 1)
                {
                    CommonTree currentArgNode = (CommonTree) argNode.getChild(childIndex++);
                    Argument arg = getFunctionArgument(currentArgNode, currentDefinition, factory, selectors);
                    functionArguments.put(arg.getName(), arg);
                }
                else
                {
                    if (definition.isMandatory())
                    {
                        // throw new CMISQueryException("Insufficient aruments
                        // for function " + ((CommonTree)
                        // functionNode.getChild(0)).getText() );
                        break;
                    }
                    else
                    {
                        // ok
                    }
                }
            }
            FunctionArgument arg = factory.createFunctionArgument(definition.getName(), function, functionArguments);
            if (!arg.isQueryable())
            {
                throw new CMISQueryException("Not all function arguments refer to orderable arguments: " + arg.getFunction().getName());
            }
            return arg;
        }
        else
        {
            throw new CMISQueryException("Invalid function argument " + argNode.getText());
        }
    }

    @SuppressWarnings("unchecked")
    private Source buildSource(CommonTree source, CMISJoinEnum joinSupport, QueryModelFactory factory)
    {
        if (source.getChildCount() == 1)
        {
            // single table reference
            CommonTree singleTableNode = (CommonTree) source.getChild(0);
            if (singleTableNode.getType() == CMISParser.TABLE)
            {
                if (joinSupport == CMISJoinEnum.NO_JOIN_SUPPORT)
                {
                    throw new UnsupportedOperationException("Joins are not supported");
                }
                CommonTree tableSourceNode = (CommonTree) singleTableNode.getFirstChildWithType(CMISParser.SOURCE);
                return buildSource(tableSourceNode, joinSupport, factory);

            }
            if (singleTableNode.getType() != CMISParser.TABLE_REF)
            {
                throw new CMISQueryException("Expecting TABLE_REF token but found " + singleTableNode.getText());
            }
            String tableName = singleTableNode.getChild(0).getText();
            String alias = "";
            if (singleTableNode.getChildCount() > 1)
            {
                alias = singleTableNode.getChild(1).getText();
            }

            CMISTypeDefinition typeDef = cmisDictionaryService.findTypeByQueryName(tableName);
            if (typeDef == null)
            {
                throw new CMISQueryException("Type is unsupported in query: " + tableName);
            }
            if (typeDef.getTypeId().getScope() != CMISScope.POLICY)
            {
                if (!typeDef.isQueryable())
                {
                    throw new CMISQueryException("Type is not queryable " + tableName + " -> " + typeDef.getTypeId());
                }
            }
            // check sub types all include in super type query
            for(CMISTypeDefinition subType : typeDef.getSubTypes(true))
            {
                if(!subType.isIncludeInSuperTypeQuery())
                {
                    throw new CMISQueryException("includeInSuperTypeQuery=falss is not support for "+tableName+ " descendant type "+subType.getQueryName());
                }
            }
            return factory.createSelector(typeDef.getTypeId().getQName(), alias);
        }
        else
        {
            if (joinSupport == CMISJoinEnum.NO_JOIN_SUPPORT)
            {
                throw new UnsupportedOperationException("Joins are not supported");
            }
            CommonTree singleTableNode = (CommonTree) source.getChild(0);
            if (singleTableNode.getType() != CMISParser.TABLE_REF)
            {
                throw new CMISQueryException("Expecting TABLE_REF token but found " + singleTableNode.getText());
            }
            String tableName = singleTableNode.getChild(0).getText();
            String alias = "";
            if (singleTableNode.getChildCount() == 2)
            {
                alias = singleTableNode.getChild(1).getText();
            }
            CMISTypeDefinition typeDef = cmisDictionaryService.findTypeByQueryName(tableName);
            if (typeDef == null)
            {
                throw new CMISQueryException("Type is unsupported in query " + tableName);
            }
            if (typeDef.getTypeId().getScope() != CMISScope.POLICY)
            {
                if (!typeDef.isQueryable())
                {
                    throw new CMISQueryException("Type is not queryable " + tableName + " -> " + typeDef.getTypeId());
                }
            }
            // check sub types all include in super type query
            for(CMISTypeDefinition subType : typeDef.getSubTypes(true))
            {
                if(!subType.isIncludeInSuperTypeQuery())
                {
                    throw new CMISQueryException("includeInSuperTypeQuery=falss is not support for "+tableName+ " descendant type "+subType.getQueryName());
                }
            }
            Source lhs = factory.createSelector(typeDef.getTypeId().getQName(), alias);

            List<CommonTree> list = (List<CommonTree>) (source.getChildren());
            for (CommonTree joinNode : list)
            {
                if (joinNode.getType() == CMISParser.JOIN)
                {
                    CommonTree rhsSource = (CommonTree) joinNode.getFirstChildWithType(CMISParser.SOURCE);
                    Source rhs = buildSource(rhsSource, joinSupport, factory);

                    JoinType joinType = JoinType.INNER;
                    CommonTree joinTypeNode = (CommonTree) joinNode.getFirstChildWithType(CMISParser.LEFT);
                    if (joinTypeNode != null)
                    {
                        joinType = JoinType.LEFT;
                    }

                    if ((joinType == JoinType.LEFT) && (joinSupport == CMISJoinEnum.INNER_JOIN_SUPPORT))
                    {
                        throw new UnsupportedOperationException("Outer joins are not supported");
                    }

                    Constraint joinCondition = null;
                    CommonTree joinConditionNode = (CommonTree) joinNode.getFirstChildWithType(CMISParser.ON);
                    if (joinConditionNode != null)
                    {
                        PropertyArgument arg1 = buildColumnReference(Equals.ARG_LHS, (CommonTree) joinConditionNode.getChild(0), factory);
                        if (!lhs.getSelectors().containsKey(arg1.getSelector()) && !rhs.getSelectors().containsKey(arg1.getSelector()))
                        {
                            throw new CMISQueryException("No table with alias " + arg1.getSelector());
                        }
                        CommonTree functionNameNode = (CommonTree) joinConditionNode.getChild(1);
                        if (functionNameNode.getType() != CMISParser.EQUALS)
                        {
                            throw new CMISQueryException("Only Equi-join is supported " + functionNameNode.getText());
                        }
                        Function function = factory.getFunction(Equals.NAME);
                        if (function == null)
                        {
                            throw new CMISQueryException("Unknown function: " + functionNameNode.getText());
                        }
                        PropertyArgument arg2 = buildColumnReference(Equals.ARG_RHS, (CommonTree) joinConditionNode.getChild(2), factory);
                        if (!lhs.getSelectors().containsKey(arg2.getSelector()) && !rhs.getSelectors().containsKey(arg2.getSelector()))
                        {
                            throw new CMISQueryException("No table with alias " + arg2.getSelector());
                        }
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                        functionArguments.put(arg1.getName(), arg1);
                        functionArguments.put(arg2.getName(), arg2);
                        joinCondition = factory.createFunctionalConstraint(function, functionArguments);
                    }

                    Source join = factory.createJoin(lhs, rhs, joinType, joinCondition);
                    lhs = join;
                }
            }

            return lhs;

        }
    }

    public PropertyArgument buildColumnReference(String argumentName, CommonTree columnReferenceNode, QueryModelFactory factory)
    {
        String cmisPropertyName = columnReferenceNode.getChild(0).getText();
        String qualifer = "";
        if (columnReferenceNode.getChildCount() > 1)
        {
            qualifer = columnReferenceNode.getChild(1).getText();
        }
        CMISPropertyDefinition propDef = cmisDictionaryService.findPropertyByQueryName(cmisPropertyName);
        if (propDef == null)
        {
            throw new CMISQueryException("Unknown column/property " + cmisPropertyName);
        }
        if (options.getQueryMode() == CMISQueryMode.CMS_STRICT)
        {
            if (!propDef.isQueryable())
            {
                throw new CMISQueryException("Column is not queryable " + qualifer + "." + cmisPropertyName);
            }
        }
        return factory.createPropertyArgument(argumentName, propDef.isQueryable(), propDef.isOrderable(), qualifer, propDef.getPropertyId().getId());
    }

}
