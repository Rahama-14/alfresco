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

import java.util.ArrayList;
import java.util.Collection;
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
import org.alfresco.repo.search.impl.parsers.FTSLexer;
import org.alfresco.repo.search.impl.parsers.FTSParser;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.ArgumentDefinition;
import org.alfresco.repo.search.impl.querymodel.Column;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.Function;
import org.alfresco.repo.search.impl.querymodel.FunctionArgument;
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
import org.alfresco.repo.search.impl.querymodel.impl.BaseComparison;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Child;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Descendant;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Equals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Exists;
import org.alfresco.repo.search.impl.querymodel.impl.functions.FTSExactTerm;
import org.alfresco.repo.search.impl.querymodel.impl.functions.FTSPhrase;
import org.alfresco.repo.search.impl.querymodel.impl.functions.FTSTerm;
import org.alfresco.repo.search.impl.querymodel.impl.functions.GreaterThan;
import org.alfresco.repo.search.impl.querymodel.impl.functions.GreaterThanOrEquals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.In;
import org.alfresco.repo.search.impl.querymodel.impl.functions.LessThan;
import org.alfresco.repo.search.impl.querymodel.impl.functions.LessThanOrEquals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Like;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Lower;
import org.alfresco.repo.search.impl.querymodel.impl.functions.NotEquals;
import org.alfresco.repo.search.impl.querymodel.impl.functions.PropertyAccessor;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Score;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Upper;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.namespace.QName;
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
    
    private static CMISScope[] STRICT_SCOPES = new CMISScope[] {CMISScope.DOCUMENT, CMISScope.FOLDER};
    private static CMISScope[] ALFRESCO_SCOPES = new CMISScope[] {CMISScope.DOCUMENT, CMISScope.FOLDER, CMISScope.POLICY};
    

    public CMISQueryParser(CMISQueryOptions options, CMISDictionaryService cmisDictionaryService, CMISJoinEnum joinSupport)
    {
        this.options = options;
        this.cmisDictionaryService = cmisDictionaryService;
        this.joinSupport = joinSupport;
        this.validScopes = (options.getQueryMode() == CMISQueryMode.CMS_STRICT) ? STRICT_SCOPES : ALFRESCO_SCOPES; 
    }

    public Query parse(QueryModelFactory factory)
    {
        CMISParser parser = null;
        try
        {
            CharStream cs = new ANTLRStringStream(options.getQuery());
            CMISLexer lexer = new CMISLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            parser = new CMISParser(tokens);
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
                constraint = buildDisjunction(orNode, factory, selectors, columns);
            }

            Query query = factory.createQuery(columns, source, constraint, orderings);

            // TODO: validate query and use of ID, function arguments matching up etc

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
    private Constraint buildDisjunction(CommonTree orNode, QueryModelFactory factory, Map<String, Selector> selectors, ArrayList<Column> columns)
    {
        List<Constraint> constraints = new ArrayList<Constraint>(orNode.getChildCount());
        for (int i = 0; i < orNode.getChildCount(); i++)
        {
            CommonTree andNode = (CommonTree) orNode.getChild(i);
            Constraint constraint = buildConjunction(andNode, factory, selectors, columns);
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
    private Constraint buildConjunction(CommonTree andNode, QueryModelFactory factory, Map<String, Selector> selectors, ArrayList<Column> columns)
    {
        List<Constraint> constraints = new ArrayList<Constraint>(andNode.getChildCount());
        for (int i = 0; i < andNode.getChildCount(); i++)
        {
            CommonTree notNode = (CommonTree) andNode.getChild(i);
            Constraint constraint = buildNegation(notNode, factory, selectors, columns);
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
    private Constraint buildNegation(CommonTree notNode, QueryModelFactory factory, Map<String, Selector> selectors, ArrayList<Column> columns)
    {
        if (notNode.getType() == CMISParser.NEGATION)
        {
            Constraint constraint = buildTest((CommonTree) notNode.getChild(0), factory, selectors, columns);
            return factory.createNegation(constraint);
        }
        else
        {
            return buildTest(notNode, factory, selectors, columns);
        }
    }

    /**
     * @param notNode
     * @param factory
     * @param selectors
     * @param columns
     * @return
     */
    private Constraint buildTest(CommonTree testNode, QueryModelFactory factory, Map<String, Selector> selectors, ArrayList<Column> columns)
    {
        if (testNode.getType() == CMISParser.DISJUNCTION)
        {
            return buildDisjunction(testNode, factory, selectors, columns);
        }
        else
        {
            return buildPredicate(testNode, factory, selectors, columns);
        }
    }

    /**
     * @param orNode
     * @param factory
     * @param selectors
     * @param columns
     * @return
     */
    private Constraint buildPredicate(CommonTree predicateNode, QueryModelFactory factory, Map<String, Selector> selectors, ArrayList<Column> columns)
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
                if(!arg.isQueryable())
                {
                    throw new CMISQueryException("The property is not queryable: "+argNode.getText());
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
            FTSQueryParser ftsQueryParser = new FTSQueryParser(cmisDictionaryService);
            Selector selector;
            if(predicateNode.getChildCount() > 1)
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
                if(selectors.size() == 1)
                {
                    selector = selectors.get(selectors.keySet().iterator().next());
                }
                else
                {
                    throw new CMISQueryException("A selector must be specified when there are two or more selectors");
                }
            }
            return ftsQueryParser.buildFTS(ftsExpression.substring(1, ftsExpression.length()-1), factory, selector, columns);
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
                        if (match == null)
                        {

                            Selector selector = selectors.get(qualifier);
                            if (selector == null)
                            {
                                throw new CMISQueryException("No selector for " + qualifier);
                            }
                            
                            CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), CMISScope.DOCUMENT, CMISScope.FOLDER);
                            if (typeDef == null)
                            {
                                throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                            }
                            CMISPropertyDefinition propDef = cmisDictionaryService.findProperty(columnName, typeDef);
                            if (propDef == null)
                            {
                                throw new CMISQueryException("Invalid column for " + typeDef.getQueryName() + "." + columnName);
                            }

                            Function function = factory.getFunction(PropertyAccessor.NAME);
                            Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, propDef.isQueryable(), propDef.isOrderable(), selector.getAlias(),
                                    propDef.getPropertyId().getQName());
                            Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                            functionArguments.put(arg.getName(), arg);

                            String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + propDef.getPropertyId().getName() : propDef.getPropertyId().getName();

                            match = factory.createColumn(function, functionArguments, alias);
                        }

                        orderColumn = match;
                    }
                    else
                    {
                        Selector selector = selectors.get(qualifier);
                        if (selector == null)
                        {
                            throw new CMISQueryException("No selector for " + qualifier);
                        }
                        
                        CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), CMISScope.DOCUMENT, CMISScope.FOLDER);
                        if (typeDef == null)
                        {
                            throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                        }
                        CMISPropertyDefinition propDef = cmisDictionaryService.findProperty(columnName, typeDef);
                        if (propDef == null)
                        {
                            throw new CMISQueryException("Invalid column for " + typeDef.getQueryName() + "." + columnName + " selector alias " + selector.getAlias());
                        }
                        
                        Function function = factory.getFunction(PropertyAccessor.NAME);
                        Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, propDef.isQueryable(), propDef.isOrderable(), selector.getAlias(),
                                propDef.getPropertyId().getQName());
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                        functionArguments.put(arg.getName(), arg);

                        String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + propDef.getPropertyId().getName() : propDef.getPropertyId().getName();

                        orderColumn = factory.createColumn(function, functionArguments, alias);
                    }

                    if (!orderColumn.isOrderable())
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
                                definition.getPropertyId().getQName());
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                        functionArguments.put(arg.getName(), arg);
                        String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + definition.getPropertyId().getName() : definition.getPropertyId().getName();
                        Column column = factory.createColumn(function, functionArguments, alias);
                        columns.add(column);
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
                        throw new CMISQueryException("No selector for " + qualifier + " in " + qualifier + ".*");
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
                                    definition.getPropertyId().getQName());
                            Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                            functionArguments.put(arg.getName(), arg);
                            String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + definition.getPropertyId().getName() : definition.getPropertyId().getName();
                            Column column = factory.createColumn(function, functionArguments, alias);
                            columns.add(column);
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
                            throw new CMISQueryException("No selector for " + qualifier);
                        }
                        
                        CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForClass(selector.getType(), validScopes);
                        if (typeDef == null)
                        {
                            throw new CMISQueryException("Type unsupported in CMIS queries: " + selector.getAlias());
                        }
                        CMISPropertyDefinition propDef = cmisDictionaryService.findProperty(columnName, typeDef);
                        if (propDef == null)
                        {
                            throw new CMISQueryException("Invalid column for " + typeDef.getQueryName() + "." + columnName);
                        }

                        Function function = factory.getFunction(PropertyAccessor.NAME);
                        Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, propDef.isQueryable(), propDef.isOrderable(), selector.getAlias(),
                                propDef.getPropertyId().getQName());
                        Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                        functionArguments.put(arg.getName(), arg);

                        String alias = (selector.getAlias().length() > 0) ? selector.getAlias() + "." + propDef.getPropertyId().getName() : propDef.getPropertyId().getName();
                        if (columnNode.getChildCount() > 1)
                        {
                            alias = columnNode.getChild(1).getText();
                        }

                        Column column = factory.createColumn(function, functionArguments, alias);
                        columns.add(column);

                    }

                    CommonTree functionNode = (CommonTree) columnNode.getFirstChildWithType(CMISParser.FUNCTION);
                    if (functionNode != null)
                    {
                        String functionName = getFunctionName((CommonTree) functionNode.getChild(0));
                        Function function = factory.getFunction(functionName);
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
                                    // throw new CMISQueryException("Insufficient aruments for function " +
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

                        String alias = query.substring(start, end + 1);
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
            if(!arg.isQueryable())
            {
                throw new CMISQueryException("Column refers to unqueryable property "+arg.getPropertyName());
            }
            if(!selectors.containsKey(arg.getSelector()))
            {
                throw new CMISQueryException("No table with alias "+arg.getSelector());
            }
            return arg;
        }
        else if (argNode.getType() == CMISParser.ID)
        {
            String id = argNode.getText();
            if (selectors.containsKey(id))
            {
                SelectorArgument arg = factory.createSelectorArgument(definition.getName(), id);
                if(!arg.isQueryable())
                {
                    throw new CMISQueryException("Selector is not queryable "+arg.getSelector());
                }
                return arg;
            }
            else
            {
                CMISPropertyDefinition propDef = cmisDictionaryService.findProperty(id, null);
                if (propDef == null || !propDef.isQueryable())
                {
                    throw new CMISQueryException("Column refers to unqueryable property " + definition.getName());
                }
                PropertyArgument arg  = factory.createPropertyArgument(definition.getName(), propDef.isQueryable(), propDef.isOrderable(), "", propDef.getPropertyId().getQName());
                return arg;
            }
        }
        else if (argNode.getType() == CMISParser.PARAMETER)
        {
            ParameterArgument arg = factory.createParameterArgument(definition.getName(), argNode.getText());
            if(!arg.isQueryable())
            {
                throw new CMISQueryException("Parameter is not queryable "+arg.getParameterName());
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
        else if (argNode.getType() == CMISParser.LIST)
        {
            ArrayList<Argument> arguments = new ArrayList<Argument>();
            for (int i = 0; i < argNode.getChildCount(); i++)
            {
                CommonTree arg = (CommonTree) argNode.getChild(i);
                arguments.add(getFunctionArgument(arg, definition, factory, selectors));
            }
            ListArgument arg = factory.createListArgument(definition.getName(), arguments);
            if(!arg.isQueryable())
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
            String functionName = getFunctionName((CommonTree) argNode.getChild(0));
            Function function = factory.getFunction(functionName);
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
                        // throw new CMISQueryException("Insufficient aruments for function " + ((CommonTree)
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
            if(!arg.isQueryable())
            {
                throw new CMISQueryException("Not all function arguments refer to orderable arguments: "+arg.getFunction().getName());
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
            
            CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForTable(tableName);
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
            CMISTypeDefinition typeDef = cmisDictionaryService.findTypeForTable(tableName);
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
                        if(!lhs.getSelectors().containsKey(arg1.getSelector()) && !rhs.getSelectors().containsKey(arg1.getSelector()))
                        {
                            throw new CMISQueryException("No table with alias "+arg1.getSelector());
                        }
                        String functionName = getFunctionName((CommonTree) joinConditionNode.getChild(1));
                        PropertyArgument arg2 = buildColumnReference(Equals.ARG_RHS, (CommonTree) joinConditionNode.getChild(2), factory);
                        if(!lhs.getSelectors().containsKey(arg2.getSelector()) && !rhs.getSelectors().containsKey(arg2.getSelector()))
                        {
                            throw new CMISQueryException("No table with alias "+arg2.getSelector());
                        }
                        Function function = factory.getFunction(functionName);
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
        CMISPropertyDefinition propDef = cmisDictionaryService.findProperty(cmisPropertyName, null);
        return factory.createPropertyArgument(argumentName, propDef.isQueryable(), propDef.isOrderable(), qualifer, propDef.getPropertyId().getQName());
    }

    public String getFunctionName(CommonTree functionNameNode)
    {
        switch (functionNameNode.getType())
        {
        case CMISParser.EQUALS:
            return Equals.NAME;
        case CMISParser.UPPER:
            return Upper.NAME;
        case CMISParser.SCORE:
            return Score.NAME;
        case CMISParser.LOWER:
            return Lower.NAME;
        default:
            throw new CMISQueryException("Unknown function: " + functionNameNode.getText());
        }
    }

}
