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
package org.alfresco.repo.search.impl.parsers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.Column;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.Function;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.LiteralArgument;
import org.alfresco.repo.search.impl.querymodel.PropertyArgument;
import org.alfresco.repo.search.impl.querymodel.QueryModelFactory;
import org.alfresco.repo.search.impl.querymodel.Selector;
import org.alfresco.repo.search.impl.querymodel.impl.functions.FTSExactTerm;
import org.alfresco.repo.search.impl.querymodel.impl.functions.FTSPhrase;
import org.alfresco.repo.search.impl.querymodel.impl.functions.FTSTerm;
import org.alfresco.repo.search.impl.querymodel.impl.functions.PropertyAccessor;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

public class FTSQueryParser
{
    public Constraint buildFTS(String ftsExpression, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Selector selector, ArrayList<Column> columns)
    {
        // TODO: Decode sql escape for '' should do in CMIS layer
        FTSParser parser = null;
        try
        {
            CharStream cs = new ANTLRStringStream(ftsExpression);
            FTSLexer lexer = new FTSLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            parser = new FTSParser(tokens);
            CommonTree ftsNode = (CommonTree) parser.ftsQuery().getTree();
            if (ftsNode.getType() == FTSParser.CONJUNCTION)
            {
                return buildFTSConjunction(ftsNode, factory, functionEvaluationContext, selector, columns);
            }
            else
            {
                return buildFTSDisjunction(ftsNode, factory, functionEvaluationContext, selector, columns);
            }
        }
        catch (RecognitionException e)
        {
            if (parser != null)
            {
                String[] tokenNames = parser.getTokenNames();
                String hdr = parser.getErrorHeader(e);
                String msg = parser.getErrorMessage(e, tokenNames);
                throw new FTSQueryException(hdr + "\n" + msg, e);
            }
            return null;
        }

    }

    private Constraint buildFTSDisjunction(CommonTree orNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Selector selector,
            ArrayList<Column> columns)
    {
        if (orNode.getType() != FTSParser.DISJUNCTION)
        {
            throw new FTSQueryException("Not disjunction " + orNode.getText());
        }
        List<Constraint> constraints = new ArrayList<Constraint>(orNode.getChildCount());
        for (int i = 0; i < orNode.getChildCount(); i++)
        {
            CommonTree subNode = (CommonTree) orNode.getChild(i);
            Constraint constraint;
            switch (subNode.getType())
            {
            case FTSParser.DISJUNCTION:
                constraint = buildFTSDisjunction(subNode, factory, functionEvaluationContext, selector, columns);
                break;
            case FTSParser.CONJUNCTION:
                constraint = buildFTSConjunction(subNode, factory, functionEvaluationContext, selector, columns);
                break;
            case FTSParser.NEGATION:
                constraint = buildFTSTest(subNode, factory, functionEvaluationContext, selector, columns);
                constraint = factory.createNegation(constraint);
                break;
            case FTSParser.DEFAULT:
                CommonTree testNode = (CommonTree) subNode.getChild(0);
                constraint = buildFTSTest(testNode, factory, functionEvaluationContext, selector, columns);
                break;
            default:
                throw new FTSQueryException("Unsupported FTS option " + subNode.getText());
            }
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

    private Constraint buildFTSConjunction(CommonTree andNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Selector selector,
            ArrayList<Column> columns)
    {
        if (andNode.getType() != FTSParser.CONJUNCTION)
        {
            throw new FTSQueryException("Not conjunction ..." + andNode.getText());
        }
        List<Constraint> constraints = new ArrayList<Constraint>(andNode.getChildCount());
        for (int i = 0; i < andNode.getChildCount(); i++)
        {
            CommonTree subNode = (CommonTree) andNode.getChild(i);
            Constraint constraint;
            switch (subNode.getType())
            {
            case FTSParser.DISJUNCTION:
                constraint = buildFTSDisjunction(subNode, factory, functionEvaluationContext, selector, columns);
                break;
            case FTSParser.CONJUNCTION:
                constraint = buildFTSConjunction(subNode, factory, functionEvaluationContext, selector, columns);
                break;
            case FTSParser.NEGATION:
                constraint = buildFTSTest(subNode, factory, functionEvaluationContext, selector, columns);
                constraint = factory.createNegation(constraint);
                break;
            case FTSParser.DEFAULT:
                CommonTree testNode = (CommonTree) subNode.getChild(0);
                constraint = buildFTSTest(testNode, factory, functionEvaluationContext, selector, columns);
                break;
            default:
                throw new FTSQueryException("Unsupported FTS option " + subNode.getText());
            }
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

    private Constraint buildFTSNegation(CommonTree notNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Selector selector,
            ArrayList<Column> columns)
    {
        switch (notNode.getType())
        {
        case FTSParser.NEGATION:
            Constraint constraint = buildFTSTest(notNode, factory, functionEvaluationContext, selector, columns);
            return factory.createNegation(constraint);
        case FTSParser.DEFAULT:
            CommonTree testNode = (CommonTree) notNode.getChild(0);
            return buildFTSTest(testNode, factory, functionEvaluationContext, selector, columns);
        default:
            throw new FTSQueryException("Unsupported FTS option " + notNode.getText());
        }

    }

    private Constraint buildFTSTest(CommonTree testNode, QueryModelFactory factory, FunctionEvaluationContext functionEvaluationContext, Selector selector,
            ArrayList<Column> columns)
    {
        String functionName;
        Function function;
        Map<String, Argument> functionArguments;
        LiteralArgument larg;
        PropertyArgument parg;
        switch (testNode.getType())
        {
        case FTSParser.DISJUNCTION:
            return buildFTSDisjunction(testNode, factory, functionEvaluationContext, selector, columns);
        case FTSParser.CONJUNCTION:
            return buildFTSConjunction(testNode, factory, functionEvaluationContext, selector, columns);
        case FTSParser.TERM:
            functionName = FTSTerm.NAME;
            function = factory.getFunction(functionName);
            functionArguments = new LinkedHashMap<String, Argument>();
            larg = factory.createLiteralArgument(FTSTerm.ARG_TERM, DataTypeDefinition.TEXT, testNode.getChild(0).getText());
            functionArguments.put(larg.getName(), larg);
            if (testNode.getChildCount() > 1)
            {
                parg = buildColumnReference(FTSTerm.ARG_PROPERTY, (CommonTree) testNode.getChild(1), factory, functionEvaluationContext, selector, columns);
                functionArguments.put(parg.getName(), parg);
            }
            return factory.createFunctionalConstraint(function, functionArguments);
        case FTSParser.EXACT_TERM:
            functionName = FTSExactTerm.NAME;
            function = factory.getFunction(functionName);
            functionArguments = new LinkedHashMap<String, Argument>();
            larg = factory.createLiteralArgument(FTSExactTerm.ARG_TERM, DataTypeDefinition.TEXT, testNode.getChild(0).getText());
            functionArguments.put(larg.getName(), larg);
            if (testNode.getChildCount() > 1)
            {
                parg = buildColumnReference(FTSExactTerm.ARG_PROPERTY, (CommonTree) testNode.getChild(1), factory, functionEvaluationContext, selector, columns);
                functionArguments.put(parg.getName(), parg);
            }
            return factory.createFunctionalConstraint(function, functionArguments);
        case FTSParser.PHRASE:
            // TODO: transform "" to " to reverse escaping
            functionName = FTSPhrase.NAME;
            function = factory.getFunction(functionName);
            functionArguments = new LinkedHashMap<String, Argument>();
            larg = factory.createLiteralArgument(FTSPhrase.ARG_PHRASE, DataTypeDefinition.TEXT, testNode.getChild(0).getText());
            functionArguments.put(larg.getName(), larg);
            if (testNode.getChildCount() > 1)
            {
                parg = buildColumnReference(FTSPhrase.ARG_PROPERTY, (CommonTree) testNode.getChild(1), factory, functionEvaluationContext, selector, columns);
                functionArguments.put(parg.getName(), parg);
            }
            return factory.createFunctionalConstraint(function, functionArguments);
        case FTSParser.SYNONYM:
        case FTSParser.FG_PROXIMITY:
        case FTSParser.FG_RANGE:
        case FTSParser.FIELD_GROUP:
        case FTSParser.FIELD_CONJUNCTION:
        case FTSParser.FIELD_DISJUNCTION:
        default:
            throw new FTSQueryException("Unsupported FTS option " + testNode.getText());
        }
    }

    public PropertyArgument buildColumnReference(String argumentName, CommonTree columnReferenceNode, QueryModelFactory factory,
            FunctionEvaluationContext functionEvaluationContext, Selector selector, ArrayList<Column> columns)
    {
        if (columnReferenceNode.getType() != FTSParser.COLUMN_REF)
        {
            throw new FTSQueryException("Not column ref  ..." + columnReferenceNode.getText());
        }
        String fieldName = columnReferenceNode.getChild(0).getText();
        if (columns != null)
        {
            for (Column column : columns)
            {
                if (column.getAlias().equals(fieldName))
                {
                    // TODO: Check selector matches ...
                    PropertyArgument arg = (PropertyArgument)column.getFunctionArguments().get(PropertyAccessor.ARG_PROPERTY);
                    fieldName = arg.getPropertyName();
                    break;
                }
            }
        }
        
        String alias = "";
        if(selector != null)
        {
            alias = selector.getAlias();
        }
        
        return factory.createPropertyArgument(argumentName, functionEvaluationContext.isQueryable(fieldName), functionEvaluationContext.isOrderable(fieldName),
                alias, fieldName);
    }

}
