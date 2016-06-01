package com.foofv.crawler.storage.mongo;

import com.foofv.crawler.similarity.structure.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by msfenn on 28/09/15.
 */
public class SQLQueryParserForMongo {

    private enum PRIORITY_STATE {
        TRUE, FALSE, ERROR
    }


    private static final String AND_REGEX = "\\sand\\s|\\sAND\\s";
    private static final String OR_REGEX = "\\sor\\s|\\sOR\\s";

    public static final String SELECT = "select";
    public static final String FROM = "from";
    public static final String WHERE = "where";

    private static final char AMPERSAND = '&';
    private static final char BEAM = '|';
    private static final char LEFT_BRACKET = '(';
    private static final char RIGHT_BRACKET = ')';

    private static final String TAIL = ";";
    private static final String ASTERISK = "*";
    private static final String END = "";
    private static final String ERROR = "error";
    private static final PRIORITY_STATE[][] precedenceMatrix = {{PRIORITY_STATE.TRUE, PRIORITY_STATE.TRUE, PRIORITY_STATE.FALSE, PRIORITY_STATE.TRUE, PRIORITY_STATE.TRUE},
            {PRIORITY_STATE.TRUE, PRIORITY_STATE.TRUE, PRIORITY_STATE.FALSE, PRIORITY_STATE.TRUE, PRIORITY_STATE.TRUE},
            {PRIORITY_STATE.FALSE, PRIORITY_STATE.FALSE, PRIORITY_STATE.FALSE, PRIORITY_STATE.TRUE, PRIORITY_STATE.TRUE},
            {PRIORITY_STATE.TRUE, PRIORITY_STATE.TRUE, PRIORITY_STATE.ERROR, PRIORITY_STATE.TRUE, PRIORITY_STATE.TRUE},
            {PRIORITY_STATE.FALSE, PRIORITY_STATE.FALSE, PRIORITY_STATE.FALSE, PRIORITY_STATE.FALSE, PRIORITY_STATE.ERROR}
    };

    private static final Map<Character, Integer> operatorIndexMap = new HashMap<>();


    static {
        operatorIndexMap.put(AMPERSAND, 0);
        operatorIndexMap.put(BEAM, 1);
        operatorIndexMap.put(LEFT_BRACKET, 2);
        operatorIndexMap.put(RIGHT_BRACKET, 3);
        operatorIndexMap.put('\0', 4);
    }

    public static Map<String, Object> extractQuerySQLStatement(String sqlStatement) {

        if (sqlStatement == null || sqlStatement.trim().length() == 0)
            return null;

        sqlStatement = sqlStatement.trim();
        sqlStatement = sqlStatement.replaceAll(AND_REGEX, "" + AMPERSAND);
        sqlStatement = sqlStatement.replaceAll(OR_REGEX, "" + BEAM);
        sqlStatement += TAIL;// just for sql expansion
        Map<String, Object> map = new HashMap<>();
        String state;
        if (sqlStatement.startsWith(SELECT) || sqlStatement.startsWith(SELECT.toUpperCase()))
            state = SELECT;
        else
            state = ERROR;
        int idx = -1;
        int curIdx;
        String data;
        while (state != END && state != ERROR) {
            switch (state) {
                case SELECT:
                    idx = sqlStatement.indexOf(FROM);
                    if (idx == -1)
                        idx = sqlStatement.indexOf(FROM.toUpperCase());
                    if (idx != -1) {
                        data = sqlStatement.substring(SELECT.length(), idx).trim();
                        if (data.equals(ASTERISK)) {
                            map.put(SELECT, null);
                        } else {
                            map.put(SELECT, data.split("\\s*,\\s*"));
                        }
                        state = FROM;
                    } else
                        state = ERROR;
                    break;
                case FROM:
                    curIdx = idx + FROM.length();
                    idx = sqlStatement.indexOf(WHERE, curIdx);
                    if (idx == -1)
                        idx = sqlStatement.indexOf(WHERE.toUpperCase(), curIdx);
                    if (idx != -1) {
                        map.put(FROM, sqlStatement.substring(curIdx, idx).trim());
                        state = WHERE;
                    } else {
                        map.put(FROM, sqlStatement.substring(curIdx, sqlStatement.length() - 1).trim());
                        state = END;
                    }
                    break;
                case WHERE:
                    curIdx = idx + WHERE.length();
                    idx = sqlStatement.indexOf(TAIL, curIdx);

                    String conditions;
                    if (idx != -1) {
                        conditions = sqlStatement.substring(curIdx, idx).trim();
                        state = END;
                    } else {
                        conditions = sqlStatement.substring(curIdx).trim();
                        state = END;
                    }

                    String prefixExpression = extractConditions(conditions);
                    Map<Character, Object> operatorOperandsMap = extractPrefixExpression(prefixExpression);

                    map.put(WHERE, operatorOperandsMap);

                    break;
            }
        }

        if (state == END)
            return map;
        else
            return null;
    }

    private static String extractConditions(String conditions) {

        Stack<Character> operatorStack = new Stack<>();
        Stack<String> operandStack = new Stack<>();
        operatorStack.push('\0');

        char curOperatorChar;
        char preOperatorChar;
        String operand1;
        String operand2;
        int idx = 0;
        int x;
        int y;
        String expression;

        for (int i = 0; i < conditions.length(); ++i) {
            curOperatorChar = conditions.charAt(i);
            if (curOperatorChar == AMPERSAND || curOperatorChar == BEAM
                    || curOperatorChar == LEFT_BRACKET || curOperatorChar == RIGHT_BRACKET) {
                expression = conditions.substring(idx, i).trim();
                idx = i + 1;
                if (expression.length() > 0)
                    operandStack.push(expression);
                x = operatorIndexMap.get(operatorStack.peek());
                y = operatorIndexMap.get(curOperatorChar);
                if (precedenceMatrix[x][y] == PRIORITY_STATE.FALSE) {//current operator is superior
                    operatorStack.push(curOperatorChar);
                } else if (precedenceMatrix[x][y] == PRIORITY_STATE.TRUE) {
                    do {
                        preOperatorChar = operatorStack.pop();
                        if (curOperatorChar == RIGHT_BRACKET && preOperatorChar == LEFT_BRACKET) {
                            break;
                        }
                        operand2 = operandStack.pop();
                        operand1 = operandStack.pop();

                        System.out.println(preOperatorChar + "" + operand1 + "," + operand2);
                        operandStack.push(preOperatorChar + "" + operand1 + "," + operand2);

                        if (operatorStack.size() == 1) {
                            operatorStack.push(curOperatorChar);
                            break;
                        }
                        x = operatorIndexMap.get(operatorStack.peek());
                    } while (precedenceMatrix[x][y] == PRIORITY_STATE.TRUE);
                } else
                    System.out.println("ERROR");//TODO: log
            }
        }

        if (idx <= conditions.length()) {
            if (!(expression = conditions.substring(idx)).isEmpty())
                operandStack.push(expression);
        }
        while (operatorStack.size() > 1) {
            preOperatorChar = operatorStack.pop();
            operand2 = operandStack.pop();
            operand1 = operandStack.pop();
            System.out.println(preOperatorChar + "" + operand1 + "," + operand2);
            operandStack.push(preOperatorChar + "" + operand1 + "," + operand2);
        }

        return operandStack.pop();
    }

    private static Map<Character, Object> extractPrefixExpression(String prefixExpression) {

        Stack<Character> operatorStack = new Stack<>();
        Stack<Object> operandStack = new Stack<>();
        Pair<Object> operandPair;
        Map<Character, Object> operatorOperandsMap;
        int idx = 0;
        char ch;
        boolean isFirstOperandAppeared = false;
        for (int i = 0; i < prefixExpression.length(); ++i) {
            ch = prefixExpression.charAt(i);
            if (ch == '&' || ch == '|') {
                idx = i + 1;
                operatorStack.push(ch);
                isFirstOperandAppeared = false;
            } else if (ch == ',') {
                operandStack.push(prefixExpression.substring(idx, i));
                idx = i + 1;
                if (isFirstOperandAppeared) {
                    do {
                        operandPair = new Pair();
                        operandPair.second = operandStack.pop();
                        operandPair.first = operandStack.pop();

                        operatorOperandsMap = new HashMap<>(1);
                        operatorOperandsMap.put(operatorStack.pop(), operandPair);
                        operandStack.push(operatorOperandsMap);
                    } while (operatorStack.empty() == false && operandStack.size() >= 2);
                } else {
                    isFirstOperandAppeared = true;
                }
            }
        }

        if (idx <= prefixExpression.length()) {
            operandStack.push(prefixExpression.substring(idx, prefixExpression.length()));
            while (operatorStack.empty() == false) {
                Pair pair = new Pair();
                pair.second = operandStack.pop();
                pair.first = operandStack.pop();

                Map<Character, Object> map = new HashMap<>(1);
                map.put(operatorStack.pop(), pair);
                operandStack.push(map);
            }
        }

        System.out.println(operandStack.size());
        assert operandStack.size() == 1;

        Object obj;
        if ((obj = operandStack.pop()).getClass() == String.class) {
            operatorOperandsMap = new HashMap<>(1);
            operatorOperandsMap.put('\0', obj);
            return operatorOperandsMap;
        } else {
            return (Map<Character, Object>) obj;
        }
    }

//    public static Bson parsePrefixExpression2MongoFilter(Map<Character, Object> operatorOperandsMap) {
//
//        Pair<Object> operandPair = null;
//        char operator = '\0';
//        Bson conditionsBson1;
//        Bson conditionsBson2;
//        Bson conditionsBson;
//
//        for (Character ch : operatorOperandsMap.keySet()) {
//            operator = ch;
//            if (ch == '\0') {
//                conditionsBson = getMongoSimpleConditionExpressionFilter((String) operatorOperandsMap.get(ch));
//                return conditionsBson;
//            } else {
//                operandPair = (Pair) operatorOperandsMap.get(ch);
//            }
//            break;
//        }
//
//        if (operandPair.first.getClass() == String.class && operandPair.second.getClass() == String.class) {
//
//            conditionsBson1 = getMongoSimpleConditionExpressionFilter((String) operandPair.first);
//            conditionsBson2 = getMongoSimpleConditionExpressionFilter((String) operandPair.second);
//
//        } else if (operandPair.first.getClass() != String.class && operandPair.second.getClass() != String.class) {
//
//            conditionsBson1 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.first);
//            conditionsBson2 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.second);
//
//        } else if (operandPair.first.getClass() != String.class) {
//
//            conditionsBson1 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.first);
//            conditionsBson2 = getMongoSimpleConditionExpressionFilter((String) operandPair.second);
//
//        } else {
//
//            conditionsBson1 = getMongoSimpleConditionExpressionFilter((String) operandPair.first);
//            conditionsBson2 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.second);
//        }
//
//        conditionsBson = MongoConstant.operatorFilterMap.get("" + operator).apply(conditionsBson1, conditionsBson2);
//
//        return conditionsBson;
//    }

//    private static Bson getMongoSimpleConditionExpressionFilter(String simpleConditionExpression) {
//
//        String[] operands = simpleConditionExpression.split(COMPARATOR_REGEX);
//        if (operands.length != 2) {
//            //TODO: log
//            return null;
//        }
//        String comparator = simpleConditionExpression.replace(operands[0], "").replace(operands[1], "");
//
//        return MongoConstant.operatorFilterMap.get(comparator).apply(operands[0].trim(), operands[1].trim());
//    }

    public static void main(String[] args) {

        String string = "(a>b & c<d)  | e=f";
//        String string = "a < b & (c>=d | e=f)";
//        System.out.println(parsePrefixExpression2MongoFilter(extractPrefixExpression(extractConditions(string))));
//        String string = "SELECT column from coll-1 where a < b and c>=d or e=f";
//        System.out.println(extractQuerySQLStatement(string));
//        System.out.println(string.replaceAll(AND, "" + AMPERSAND).replaceAll(OR, "" + BEAM));
//        extractConditions("a & b & c");
//        System.out.println("result: " + extractConditions("a & (b & (c | d)) & e"));
    }
}
