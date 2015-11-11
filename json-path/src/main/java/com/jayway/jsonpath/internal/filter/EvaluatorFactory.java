package com.jayway.jsonpath.internal.filter;

import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Predicate;

import java.util.HashMap;
import java.util.Map;

public class EvaluatorFactory {

    private static final Map<RelationalOperator, Evaluator> evaluators = new HashMap<RelationalOperator, Evaluator>();

    static {
        evaluators.put(RelationalOperator.EXISTS, new ExistsEvaluator());
        evaluators.put(RelationalOperator.NE, new NotEqualsEvaluator());
        evaluators.put(RelationalOperator.EQ, new EqualsEvaluator());
        evaluators.put(RelationalOperator.LT, new LessThanEvaluator());
        evaluators.put(RelationalOperator.LTE, new LessThanEqualsEvaluator());
        evaluators.put(RelationalOperator.GT, new GreaterThanEvaluator());
        evaluators.put(RelationalOperator.GTE, new GreaterThanEqualsEvaluator());
        evaluators.put(RelationalOperator.REGEX, new RegexpEvaluator());
        evaluators.put(RelationalOperator.SIZE, new SizeEvaluator());
        evaluators.put(RelationalOperator.NOT_EMPTY, new NotEmptyEvaluator());
        evaluators.put(RelationalOperator.IN, new InEvaluator());
        evaluators.put(RelationalOperator.NIN, new NotInEvaluator());
        evaluators.put(RelationalOperator.ALL, new AllEvaluator());
        evaluators.put(RelationalOperator.CONTAINS, new ContainsEvaluator());
        evaluators.put(RelationalOperator.MATCHES, new PredicateMatchEvaluator());
        evaluators.put(RelationalOperator.TYPE, new TypeEvaluator());
    }

    public static Evaluator createEvaluator(RelationalOperator operator){
        return evaluators.get(operator);
    }

    private static class ExistsEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(!left.isBooleanNode() && !right.isBooleanNode()){
                throw new JsonPathException("Failed to evaluate exists expression");
            }
            return left.asBooleanNode().getBoolean() == right.asBooleanNode().getBoolean();
        }
    }

    private static class NotEqualsEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            return !left.equals(right);
        }
    }

    private static class EqualsEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(left.isJsonNode() && right.isJsonNode()){
                return left.asJsonNode().equals(right.asJsonNode(), ctx);
            } else {
                return left.equals(right);
            }
        }
    }

    private static class TypeEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            return right.asClassNode().getClazz() == left.type(ctx);
        }
    }

    private static class LessThanEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(left.isNumberNode() && right.isNumberNode()){
                return left.asNumberNode().getNumber().compareTo(right.asNumberNode().getNumber()) < 0;
            } if(left.isStringNode() && right.isStringNode()){
                return left.asStringNode().getString().compareTo(right.asStringNode().getString()) < 0;
            }
            return false;
        }
    }

    private static class LessThanEqualsEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(left.isNumberNode() && right.isNumberNode()){
                return left.asNumberNode().getNumber().compareTo(right.asNumberNode().getNumber()) <= 0;
            } if(left.isStringNode() && right.isStringNode()){
                return left.asStringNode().getString().compareTo(right.asStringNode().getString()) <= 0;
            }
            return false;
        }
    }

    private static class GreaterThanEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(left.isNumberNode() && right.isNumberNode()){
                return left.asNumberNode().getNumber().compareTo(right.asNumberNode().getNumber()) > 0;
            } else if(left.isStringNode() && right.isStringNode()){
                return left.asStringNode().getString().compareTo(right.asStringNode().getString()) > 0;
            }
            return false;
        }
    }

    private static class GreaterThanEqualsEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(left.isNumberNode() && right.isNumberNode()){
                return left.asNumberNode().getNumber().compareTo(right.asNumberNode().getNumber()) >= 0;
            } else if(left.isStringNode() && right.isStringNode()){
                return left.asStringNode().getString().compareTo(right.asStringNode().getString()) >= 0;
            }
            return false;
        }
    }

    private static class SizeEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            int expectedSize = right.asNumberNode().getNumber().intValue();

            if(left.isStringNode()){
                return left.asStringNode().length() == expectedSize;
            } else if(left.isJsonNode()){
                return left.asJsonNode().length(ctx) == expectedSize;
            }
            return false;
        }
    }

    private static class NotEmptyEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(left.isStringNode()){
                return !left.asStringNode().isEmpty();
            } else if(left.isJsonNode()){
                return !left.asJsonNode().isEmpty(ctx);
            }
            return false;
        }
    }

    private static class InEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            ValueNode.ValueListNode valueListNode = right.asValueListNode();
            return valueListNode.contains(left);
        }
    }

    private static class NotInEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            return !evaluators.get(RelationalOperator.IN).evaluate(left, right, ctx);
        }
    }

    private static class AllEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            ValueNode.ValueListNode requiredValues = right.asValueListNode();

            if(left.isJsonNode()){
                ValueNode valueNode = left.asJsonNode().asValueListNode(ctx); //returns UndefinedNode if conversion is not possible
                if(valueNode.isValueListNode()){
                    ValueNode.ValueListNode shouldContainAll = valueNode.asValueListNode();
                    for (ValueNode required : requiredValues) {
                        if(!shouldContainAll.contains(required)){
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }

    private static class ContainsEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(left.isStringNode() && right.isStringNode()){
                return left.asStringNode().contains(right.asStringNode().getString());
            } else if(left.isJsonNode()){
                ValueNode valueNode = left.asJsonNode().asValueListNode(ctx);
                if(valueNode.isUndefinedNode()) return false;
                else {
                    boolean res = valueNode.asValueListNode().contains(right);
                    return res;
                }
            }
            return false;
        }
    }

    private static class PredicateMatchEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            return right.asPredicateNode().getPredicate().apply(ctx);
        }
    }

    private static class RegexpEvaluator implements Evaluator {
        @Override
        public boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx) {
            if(!(left.isPatternNode() ^ right.isPatternNode())){
                return false;
            }
            if(!(left.isStringNode() ^ right.isStringNode())){
                return false;
            }
            ValueNode.PatternNode patternNode = left.isPatternNode() ? left.asPatternNode() : right.asPatternNode();
            ValueNode.StringNode stringNode = left.isStringNode() ? left.asStringNode() : right.asStringNode();

            return patternNode.getCompiledPattern().matcher(stringNode.getString()).matches();
        }
    }
}