# Toy Calculator Language
This is just a simple calculator language that uses the Shunting Yard Algorithm! \:)\
This little playground also has a bunch of QoL stuff for users.

### Syntax:
`<char>` - Any single character is a variable\
`<char>[<char>*] -> <expr>` - Syntax for functions, parameters are optional\
`<function name> <- <variable/number>*` - Function call

### Commands:
- `help/h`: prints all the commands
- `exit/quit/q`: exits the playground
- `test/t`: runs test cases
- `clear/c`: clears the terminal
- `reset/r`: resets the variables and functions
- `debug/d`: toggles debug mode (logs every evaluation step)
- `var/variables/v`: prints all the current variables and functions
- `file/f`: evaluates every line of a given file

### Helpful Tips:
- To get negative numbers on their own, wrap them in parenthesis: `(-1)`
- Assignments can be nested, but *must* be wrapped in parenthesis to work: `A = (B = 3) * 2`
- Variables and number literals on their own are valid expressions: `A`, `1`, `-1`
- Function calls can be nested like any other operation: `f <- (f <- 2)`
- Function definitions are the only pieces of this language that do NOT have the ability to be nested: `f[x] -> (g[x] -> x + 2) + 2` (Invalid!)
