import java.util.*;
import java.util.function.Function;

public class ShuntingYardAlgorithm {

    public enum Colors {
        ERROR("\u001B[31m"),
        SUCCESS("\u001B[32m"),
        WARNING("\u001B[33m"),
        DEBUG("\u001B[34m"),
        INFO("\u001B[35m"),
        DEFAULT("\u001B[36m"),
        NOTICE("\u001B[37m"),
        RESET("\033[0m");

        private String c;
        private Colors(String color) { this.c = color; }
        public String toString() { return c; }
    }

    public static void log(String message) {
        System.out.println(Colors.DEBUG + message + Colors.RESET);
    }

    public enum Operations { ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO }

    static Map<String, Integer> precedence = new HashMap<String, Integer>();
    static {
        precedence.put("=", 0); // goes last
        precedence.put("+", 1);
        precedence.put("-", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        precedence.put("%", 2);
    }

    public static HashMap<String, Integer> variables = new HashMap<String, Integer>();

    public static int evaluateExpression(String expr) {
        Queue<String> postfix = infixToPostfix(expr);
        int result = new Value(evaluatePostfix(postfix)).getValue();
        return result;
    }

    private static Queue<String> infixToPostfix(String expr) {
        Queue<String> output = new LinkedList<String>();
        Stack<String> operators = new Stack<String>();
        List<String> tokens = tokenize(expr);

        for (String token : tokens) {
            if (token.matches("\\d+|-\\d+")) { // If token is a number (including negative)
                output.add(token);
            } else if (token.equals("(")) {
                operators.push(token); // Push opening parenthesis to the stack
            } else if (token.equals(")")) {
                // Pop operators until an opening parenthesis is found
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                operators.pop(); // Remove the opening parenthesis
            } else if (isOperator(token.charAt(0))) { // Operator
                // Pop operators with higher or equal precedence
                while (!operators.isEmpty() && !operators.peek().equals("(") &&
                        precedence.getOrDefault(operators.peek(), 0) >= precedence.get(token)) {
                    output.add(operators.pop());
                }
                operators.push(token); // Push the current operator
            } else if (isValidIdent(token)) { // If token is a variable
                output.add(token);
            } else {
                throw new IllegalArgumentException("Invalid token: " + token);
            }
        }

                while (!operators.isEmpty()) { output.add(operators.pop()); }
        return output;
    }

    private static boolean isOperator(char c) { return c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')' || c == '='; }
    private static boolean isValidIdent(String token) { return token.matches("[a-zA-Z]");}

    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder numberBuffer = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Build the number token
            if (Character.isDigit(c) || (c == '-' && (i == 0 || expr.charAt(i - 1) == '('))) {
                // Include '-' if it's part of a negative number
                numberBuffer.append(c);
            } else {
                // If there's a number in the buffer, add it as a token
                if (numberBuffer.length() > 0) {
                    tokens.add(numberBuffer.toString());
                    numberBuffer.setLength(0); // Clear the buffer
                }

                // Add operators and parentheses as tokens
                if (isOperator(c)) {
                    tokens.add(String.valueOf(c));
                }
                if (Character.isWhitespace(c)) {
                    continue; // Ignore whitespace
                }
                if (isValidIdent(String.valueOf(c))) {
                    tokens.add(String.valueOf(c)); // Add variable to tokens
                }
            }
        }

        // Add any remaining number in the buffer as a token
        if (numberBuffer.length() > 0) {
            tokens.add(numberBuffer.toString());
        }

        return tokens;
    }

    // variables
    // integers
    // operators

    static class Value {
        String ident;
        Integer value;

        public Value(String token) {
            if (token.matches("\\d+|-\\d+")) { // If token is a number (including negative)
                this.value = Integer.parseInt(token);
                this.ident = null;
            } else if (token.matches("[a-zA-Z]")) { // If token is a variable
                this.ident = token;
                this.value = null;
            } else {
                throw new IllegalArgumentException("Invalid token: " + token);
            }
        }

        public boolean isNumber() {
            return value != null;
        }

        public boolean isVariable() {
            return ident != null;
        }

        public int getValue() {
            if (isNumber()) return value;
            if (isVariable()) {
                if (variables.containsKey(ident)) {
                    return variables.get(ident);
                } else {
                    throw new IllegalArgumentException("Variable '" + ident + "' is not defined.");
                }
            }
            throw new IllegalArgumentException("Invalid value: " + (ident != null ? ident : value));
        }
    }

    private static String evaluatePostfix(Queue<String> postfix) {
        Stack<String> stack = new Stack<String>();

        while (!postfix.isEmpty()) {
            String token = postfix.poll();
            if (token.matches("\\d+|[a-zA-Z]|-\\d+")) { // If token is a number (including negative) or variable
                stack.push(token);
            } else { // Operator
                Value b = new Value(stack.pop());
                Value a = null; // Initialize 'a' as null
                if (token.equals("=")) { // For assignment, pop 'a' only if it's a variable
                    if (!stack.isEmpty()) {
                        a = new Value(stack.pop());
                        if (!a.isVariable()) {
                            throw new IllegalArgumentException("Invalid assignment: left-hand side must be a variable.");
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid assignment: missing left-hand side.");
                    }
                } else { // For other operators, pop 'a' as needed
                    if (!stack.isEmpty()) {
                        a = new Value(stack.pop());
                    } else {
                        throw new IllegalArgumentException("Invalid expression: missing operand for operator '" + token + "'");
                    }
                }

                switch (token) {
                    case "+" -> stack.push(Integer.toString(a.getValue() + b.getValue()));
                    case "-" -> stack.push(Integer.toString(a.getValue() - b.getValue()));
                    case "*" -> stack.push(Integer.toString(a.getValue() * b.getValue()));
                    case "/" -> {
                        if (b.getValue() == 0) throw new ArithmeticException("Division by zero is not allowed.");
                        stack.push(Integer.toString(a.getValue() / b.getValue()));
                    }
                    case "%" -> {
                        if (b.getValue() == 0) throw new ArithmeticException("Modulo by zero is not allowed.");
                        stack.push(Integer.toString(a.getValue() % b.getValue()));
                    }
                    case "=" -> {
                        int valueToAssign = b.getValue(); // Fetch the value of 'b', whether it's a variable or a number
                        variables.put(a.ident, valueToAssign); // Store the value in the variable map
                        stack.push(Integer.toString(valueToAssign)); // Push the assigned value back to the stack as a number
                    }
                    default -> throw new IllegalArgumentException("Invalid operator: " + token);
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression: stack state is incorrect after evaluation.");
        }

        return stack.pop();
    }

    public static void main(String[] args) {
        String[] testCases = {"1 + 2", "3 - 4", "5 * 6", "7 / 8", "9 % 10", "2 * ( 1 + 4 / 2 )", "1+1", "1/0", "10 % 0", "0/1"};
        Integer[] expectedResults = {3, -1, 30, 0, 9, 6, 2, 0, 0, 0}; // Note: Division by zero and modulo by zero are handled as exceptions, so they will fail, 0 is placeholder

        prettyPrint(testCases);
        evaulateCases(testCases, expectedResults, testCase -> evaluateExpression((String) testCase));

        Scanner scanner = new Scanner(System.in);
        System.out.println(Colors.NOTICE + "Enter an expression to evaluate (or 'exit'/'q' to quit):" + Colors.RESET);
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("q")) break;
            try {
                int result = evaluateExpression(input);
                System.out.println(Colors.SUCCESS + "Result: " + Colors.INFO + result + Colors.RESET);
            } catch (Exception e) {
                System.out.println(Colors.ERROR + "Error: " + Colors.INFO + e.getMessage() + Colors.RESET);
            }
        }
        System.out.println(Colors.NOTICE + "Exiting the program." + Colors.RESET);
        scanner.close();
    }

    public static <T> void prettyPrint(T[] testCases) {
        System.out.println(Colors.DEBUG + "Test Cases:" + Colors.RESET);
        for (int i = 0; i < testCases.length; i++) {
            System.out.println("  " + Colors.INFO + i + Colors.DEBUG + " : " + Colors.INFO + testCases[i].toString() + Colors.RESET);
        }
    }

    public static <T, R> void evaulateCases(T[] cases, R[] expectedResults, Function<T, R> operation) {
        for(int i = 0; i < cases.length; i++) {
            T testCase = cases[i];
            R expectedResult = expectedResults[i];
            try {
                R result = operation.apply((T) testCase);
                assert (result.equals(expectedResult)) : Colors.ERROR + "Test case failed. Expected: " + Colors.INFO + expectedResult.toString() + Colors.ERROR + ", but got: " + Colors.INFO + result.toString() + Colors.RESET;
                System.out.println(Colors.SUCCESS + "Test case passed." + Colors.RESET);
            } catch (Exception e) {
                System.out.println(Colors.ERROR + "Test case failed with exception: " + Colors.INFO + e.getMessage() + Colors.RESET);
            }
        }
    }
}