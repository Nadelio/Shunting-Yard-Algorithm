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
        if(!DEBUG_MODE) return;
        System.out.println(Colors.DEBUG + message + Colors.RESET);
    }

    public enum Operations { ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO }

    static Map<String, Integer> precedence = new HashMap<String, Integer>();
    static {
        precedence.put("=", 0); // goes last, unless in parentheses
        precedence.put("+", 1);
        precedence.put("-", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        precedence.put("%", 2);
    }

    public static HashMap<String, Integer> variables = new HashMap<String, Integer>();
    static Map<String, FunctionDefinition> functions = new HashMap<>();

    static class FunctionDefinition { //? maybe in the future differentiate between functions that return a value and functions that return a function
        List<String> parameters;
        String expression;

        public FunctionDefinition(List<String> parameters, String expression) {
            this.parameters = parameters;
            this.expression = expression;
        }
    }

    public static int evaluateExpression(String expr) {
        if (expr.matches("[a-zA-Z]\\s*\\[\\s*.*\\s*\\]\\s*->\\s*.*")) {
            expr = expr.replaceAll("\\s+", ""); // Remove all whitespace for function definition
            // Parse function definition
            String functionName = expr.substring(0, expr.indexOf('[')).trim();
            String params = expr.substring(expr.indexOf('[') + 1, expr.indexOf(']')).trim();
            String expression = expr.substring(expr.indexOf("->") + 2).trim();

            List<String> parameters = Arrays.asList(params.split("\\s*,\\s*"));
            functions.put(functionName, new FunctionDefinition(parameters, expression));
            System.out.println(Colors.SUCCESS + "Function '" + functionName + "' defined." + Colors.RESET);
            return 0; // No result to return for function definition
        } else {
            Queue<String> postfix = infixToPostfix(expr);
            int result = new Value(evaluatePostfix(postfix)).getValue();
            return result;
        }
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
            } else if (isOperator(token)) { // Operator
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

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')' || c == '=';
    }

    private static boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") ||
               token.equals("%") || token.equals("=") || token.equals("<-");
    }

    private static boolean isValidIdent(String token) { return token.matches("[a-zA-Z]");}

    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder numberBuffer = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Build the number token
            if (Character.isDigit(c) || (c == '-' && (i == 0 || expr.charAt(i - 1) == '('))) {
                numberBuffer.append(c);
            } else {
                // If there's a number in the buffer, add it as a token
                if (numberBuffer.length() > 0) {
                    tokens.add(numberBuffer.toString());
                    numberBuffer.setLength(0); // Clear the buffer
                }

                // Handle the `<-` operator
                if (c == '<' && i + 1 < expr.length() && expr.charAt(i + 1) == '-') {
                    tokens.add("<-");
                    i++; // Skip the next character ('-')
                } else if (isOperator(c)) {
                    tokens.add(String.valueOf(c));
                } else if (Character.isWhitespace(c)) {
                    continue; // Ignore whitespace
                } else if (isValidIdent(String.valueOf(c))) {
                    tokens.add(String.valueOf(c)); // Add variable or function name to tokens
                } else {
                    throw new IllegalArgumentException("Invalid token: " + c);
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
        Stack<String> stack = new Stack<>();

        log("Expression: " + Colors.INFO + postfix.toString());

        while (!postfix.isEmpty()) {
            String token = postfix.poll();
            log("Processing token: " + Colors.INFO + token);
            log("Stack state: " + Colors.INFO + stack.toString());

            if (token.matches("\\d+|[a-zA-Z]|-\\d+")) { // If token is a number (including negative) or variable
                stack.push(token);
            } else if (token.equals("<-")) { // Function call using the new syntax
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: missing function or argument for '<-' operator.");
                }

                String argument = stack.pop();
                String functionName = stack.pop();

                if (!functions.containsKey(functionName)) {
                    throw new IllegalArgumentException("Function '" + functionName + "' is not defined.");
                }

                FunctionDefinition function = functions.get(functionName);

                // Resolve the argument (if it's a variable, get its value)
                String resolvedArgument;
                if (variables.containsKey(argument)) {
                    resolvedArgument = String.valueOf(variables.get(argument).intValue());
                } else if (argument.matches("-?\\d+")) { // If it's already a number
                    resolvedArgument = argument;
                } else {
                    throw new IllegalArgumentException("Invalid argument: '" + argument + "' is not a valid number or variable.");
                }

                List<String> arguments = Arrays.asList(resolvedArgument);

                if (arguments.size() != function.parameters.size()) {
                    throw new IllegalArgumentException("Function '" + functionName + "' expects " + function.parameters.size() + " arguments, but got " + arguments.size() + ".");
                }

                // Temporarily store variable values
                HashMap<String, Integer> tempVariables = new HashMap<String, Integer>(variables);

                // Assign arguments to parameters
                for (int i = 0; i < function.parameters.size(); i++) {
                    variables.put(function.parameters.get(i), Integer.parseInt(arguments.get(i).trim()));
                }

                // Evaluate the function expression
                int result = evaluateExpression(function.expression);

                // Restore original variables
                variables = tempVariables;

                // Push the result of the function evaluation onto the stack
                stack.push(Integer.toString(result)); //? this would need to change if I allow functions to return functions
            } else { // Operator
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: missing operand for operator '" + token + "'");
                }

                Value b = new Value(stack.pop());
                Value a = new Value(stack.pop());

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
                        if (!a.isVariable()) {
                            throw new IllegalArgumentException("Invalid assignment: left-hand side must be a variable.");
                        }
                        int valueToAssign = b.getValue();
                        variables.put(a.ident, valueToAssign);
                        stack.push(Integer.toString(valueToAssign));
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

    public static boolean DEBUG_MODE = true;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.println(Colors.NOTICE + "Enter an expression to evaluate (or 'exit'/'q' to quit):" + Colors.RESET);
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")) break;
            if (input.equalsIgnoreCase("clear") || input.equalsIgnoreCase("c")) {
                System.out.print("\033[H\033[2J"); // ANSI escape code to clear the console
                System.out.flush();
                System.out.println(Colors.DEBUG + "Console cleared." + Colors.RESET);
                continue;
            }
            if (input.equalsIgnoreCase("reset") || input.equalsIgnoreCase("r")) {
                variables.clear();
                System.out.println(Colors.DEBUG + "Variables reset." + Colors.RESET);
                functions.clear();
                System.out.println(Colors.DEBUG + "Functions reset." + Colors.RESET);
                continue;
            }
            if (input.equalsIgnoreCase("test") || input.equalsIgnoreCase("t")) {
                String[] testCases = {"1 + 2", "3 - 4", "5 * 6", "7 / 8", "9 % 10", "2 * ( 1 + 4 / 2 )", "1+1", "1/0", "10 % 0", "0/1", "A = 2", "B = A + 2", "A = B", "A = (B = 3) * 2", "A", "B", "A = (-1)"};
                Integer[] expectedResults = {3, -1, 30, 0, 9, 6, 2, 0, 0, 0, 2, 4, 4, 6, 6, 3, -1}; // Note: Division by zero and modulo by zero are handled as exceptions, so they will fail, 0 is placeholder
        
                prettyPrint(testCases);
                evaulateCases(testCases, expectedResults, testCase -> evaluateExpression((String) testCase));
                continue;
            }
            if (input.isEmpty()) continue; // Ignore empty input
            if (input.equalsIgnoreCase("debug") || input.equalsIgnoreCase("d")) {
                DEBUG_MODE = !DEBUG_MODE; // Toggle debug mode
                System.out.println(Colors.DEBUG + "Debugging mode toggled." + Colors.RESET);
                continue;
            }
            if (input.equalsIgnoreCase("help") || input.equalsIgnoreCase("h")) {
                System.out.println(Colors.NOTICE + "Available commands:" + Colors.RESET);
                System.out.println(Colors.INFO + "  exit/quit/q - Quit the program" + Colors.RESET);
                System.out.println(Colors.INFO + "  clear/c - Clear the console" + Colors.RESET);
                System.out.println(Colors.INFO + "  reset/r - Reset variables and functions" + Colors.RESET);
                System.out.println(Colors.INFO + "  test/t - Run test cases" + Colors.RESET);
                System.out.println(Colors.INFO + "  debug/d - Toggle debugging mode" + Colors.RESET);
                System.out.println(Colors.INFO + "  help/h - Show this help message" + Colors.RESET);
                System.out.println(Colors.INFO + "  variables/var/v - Show current variables and functions" + Colors.RESET);
                System.out.println(Colors.INFO + "  file/f - Evaluate the lines of a file" + Colors.RESET);
                continue;
            }
            if (input.equalsIgnoreCase("variables") || input.equalsIgnoreCase("var") || input.equalsIgnoreCase("v")) {
                System.out.println(Colors.DEBUG + "Current variables:" + Colors.RESET);
                for(Map.Entry<String, Integer> entry : variables.entrySet()) {
                    System.out.println("  " + Colors.INFO + entry.getKey() + Colors.DEBUG + " : " + Colors.INFO + entry.getValue() + Colors.RESET);
                }
                System.out.println(Colors.DEBUG + "Current functions:" + Colors.RESET);
                for(Map.Entry<String, FunctionDefinition> entry : functions.entrySet()) {
                    System.out.println("  " + Colors.INFO + entry.getKey() + Colors.DEBUG + " : " + Colors.INFO + entry.getValue().expression + Colors.RESET);
                }
                continue;
            }
            if (input.equalsIgnoreCase("file") || input.equalsIgnoreCase("f")) {
                System.out.println(Colors.NOTICE + "Enter the file path:" + Colors.RESET);
                String filePath = scanner.nextLine().trim();
                try (Scanner fileScanner = new Scanner(new java.io.File(filePath))) {
                    while (fileScanner.hasNextLine()) {
                        String line = fileScanner.nextLine().trim();
                        if (!line.isEmpty()) {
                            System.out.println(Colors.DEBUG + "Evaluating line: " + Colors.INFO + line + Colors.RESET);
                            int result = evaluateExpression(line);
                            System.out.println(Colors.SUCCESS + "Result: " + Colors.INFO + result + Colors.RESET);
                        }
                    }
                } catch (Exception e) {
                    System.out.println(Colors.ERROR + "Error when reading from file: " + Colors.INFO + filePath + Colors.ERROR + " : " + Colors.INFO + e.getMessage() + Colors.RESET);
                }
                continue;
            }
            try {
                int result = evaluateExpression(input);
                System.out.println(Colors.SUCCESS + "Result: " + Colors.INFO + result + Colors.RESET);
            } catch (Exception e) {
                System.out.println(Colors.ERROR + "Error: " + Colors.INFO + e.getMessage() + Colors.RESET);
            }
        }
        System.out.println(Colors.DEBUG + "Exiting the program..." + Colors.RESET);
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