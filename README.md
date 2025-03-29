# Toy Calculator Language
This is just a simple calculator language that uses the Shunting Yard Algorithm \:)\
I might add a way to read from files in the future \:P\
It would probably read line-by-line like a scripting language, because that's easy, but who knows!\
This little playground also has a bunch of QoL stuff for users.

### Commands:
- `help/h`: prints all the commands
- `exit/quit/q`: exits the playground
- `test/t`: runs test cases
- `clear/c`: clears the terminal
- `reset/r`: resets the variables
- `debug/d`: toggles debug mode (logs every evaluation step)
- `var/variables/v`: prints all the current variables

### Helpful Tips:
- To get negative numbers on their own, wrap them in parenthesis: `(-1)`
- Assignments can be nested, but *must* be wrapped in parenthesis to work: `A = (B = 3) * 2`
- Variables and number literals on their own are valid expressions: `A`, `1`, `-1`