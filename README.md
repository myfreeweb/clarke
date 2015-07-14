# Clarke

A simple, statically typed [concatenative] programming language for the JVM.

The compiler is implemented in under 1000 lines of Java, which makes it perfect for learning about JVM bytecode.

The language currently supports:

- generating classes with static methods
- calling static methods
- literals for Java primitive types and Strings
- some built-in operations (stack manipulation, boolean logic, arithmetic)
- conditionals and loops

[concatenative]: http://concatenative.org/wiki/view/Concatenative%20language

```
class Hello;
main ∷ java.lang.String[] = 2 2 * println;
```

## Usage

```bash
$ ./gradlew shadowJar

$ java -jar build/libs/clarke-1.0-SNAPSHOT-all.jar example.clarke
$ java Hello
$ javap -c Hello
```

## Syntax

```
// Files contain class declarations.
class Hello;

// Classes contain method declarations.
// Only static methods are supported right now.

// The method syntax is simple: name ∷ argument types → return type = expressions;
//
// Expressions are:
// - literals (push onto the stack)
// - loops and conditionals
// - built-in operators
// - method calls
//
// Built-in operators:
//    +  -  *  /  %
//    ¬  ∧  ∨  ==  ≠  <  >  ≤  ≥
//    dup  swap  pop  over  println

multiplyAndSquare ∷ int int → int = * dup *;

// Parts of the type signature can be left out when you return void or take no args:

hi = "Hello world!" println;

returnNumber ∷ → int = 2 2 *;

eatNumber ∷ int = pop;

// Conditionals look like this:

lengthCheck ∷ int → java.lang.String = 15 ≥ { "Your password is too short" } { "OK" } if;

enlargeIfSmall ∷ int → int = dup 10 ≤ { 2 * } when;

enlargeUnlessBig ∷ int → int = dup 10 ≥ { 2 * } unless;

// And loops look like this:

gcd ∷ int int → int = { swap over % } { dup 0 ≠ } while pop;

// The compiler is double-pass so this works:

class Program;
main ∷ java.lang.String[] = 20 5 MathStuff.gcd println;

class MathStuff;
gcd ∷ int int → int = { swap over % } { dup 0 ≠ } while pop;
```

## The Name

Well, there was [a concatenative language called Raven](http://aerosuidae.net/raven.html) which obviously must be named after [this Raven](http://the100.wikia.com/wiki/Raven_Reyes) so where's [Clarke](http://the100.wikia.com/wiki/Clarke_Griffin)? *(warning: spoliers for The 100 on both links!)*
