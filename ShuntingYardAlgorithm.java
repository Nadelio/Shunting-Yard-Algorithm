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

    public enum Operations { ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO }

    static Map<String, Integer> precedence = new HashMap<String, Integer>();
    static {
        precedence.put("+", 1);
        precedence.put("-", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        precedence.put("%", 2);
    }

    public static int evaluateExpression(String expr) {
        Queue<String> postfix = infixToPostfix(expr);
        return evaluatePostfix(postfix);
    }

    private static Queue<String> infixToPostfix(String expr) {
        Queue<String> output = new LinkedList<>();
        Stack<String> operators = new Stack<>();
        List<String> tokens = tokenize(expr);

        for (String token : tokens) {
            if (token.matches("\\d+")) { // If token is a number
                output.add(token);
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) { output.add(operators.pop()); }
                operators.pop(); // Remove the "("
            } else { // Operator
                while (!operators.isEmpty() && precedence.getOrDefault(operators.peek(), 0) >= precedence.get(token)) {
                    output.add(operators.pop());
                }
                operators.push(token);
            }
        }

        while (!operators.isEmpty()) { output.add(operators.pop()); }

        return output;
    }

    private static boolean isOperator(char c) { return c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')'; }

    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder numberBuffer = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Build the number token
            if (Character.isDigit(c)) {
                numberBuffer.append(c);
            } else {
                // If there's a number in the buffer, add it as a token
                if (numberBuffer.length() > 0) {
                    tokens.add(numberBuffer.toString());
                    numberBuffer.setLength(0); // Clear the buffer
                }

                // Add operators and parentheses as tokens
                if (isOperator(c)) { tokens.add(String.valueOf(c)); }
            }
        }

        // Add any remaining number in the buffer as a token
        if (numberBuffer.length() > 0) { tokens.add(numberBuffer.toString()); }

        return tokens;
    }

    private static int evaluatePostfix(Queue<String> postfix) {
        Stack<Integer> stack = new Stack<>();

        while (!postfix.isEmpty()) {
            String token = postfix.poll();
            if (token.matches("\\d+")) { // If token is a number
                stack.push(Integer.parseInt(token));
            } else { // Operator
                int b = stack.pop();
                int a = stack.pop();
                switch (token) {
                    case "+" -> stack.push(a + b);
                    case "-" -> stack.push(a - b);
                    case "*" -> stack.push(a * b);
                    case "/" -> {
                        if (b == 0) throw new ArithmeticException("Division by zero is not allowed.");
                        stack.push(a / b);
                    }
                    case "%" -> {
                        if (b == 0) throw new ArithmeticException("Modulo by zero is not allowed.");
                        stack.push(a % b);
                    }
                }
            }
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