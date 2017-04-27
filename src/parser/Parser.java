/**
 * Implementation of a recursive descent parser for the Java programming
 * language. 
 * 
 * @author Michael Smith and Nathan Jean
 */

package parser;

import java.util.ArrayDeque;

import interfaces.ParserInterface;
import types.InvalidInputException;
import types.Lexeme;
import types.Token;

public class Parser implements ParserInterface{

	LexicalAnalyzer lex;
	Lexeme nextLexeme;
	int indentationLevel;
	String returnString; //string to return (only needed for GUI)
	ArrayDeque<String> outputQueue;
	int lineNumber;

	/**
	* Constructor creates the lexical analyzer and initializes nextLexeme, given an input string
	*
	* @param inputString
	 * @throws InvalidInputException
	*/
	public Parser(String inputString) {
		lex = new LexicalAnalyzer(inputString);
		nextLexeme = lex.nextLexeme();
		indentationLevel = 0;
		outputQueue = new ArrayDeque<String>();
		lineNumber = 1;
	} // end constructor
	
	public String getErrorMessage() {
		return String.format("ERROR: Line %d: Invalid input: %s\n", lex.getLineNumber(), nextLexeme.getLexeme());
	}
	
	@Override
	// begin the recursive descent process
	public void start() throws InvalidInputException {
		program(); // <program>
	} // end start()

	// <qualified_identifier> = <identifier> {"." <identifier>};
	private void qualifiedIdentifier() throws InvalidInputException {
		printIndented("Enter <qualified_identifier>", 1);

		processLexeme(Token.IDENTIFIER);

		while (nextLexeme.getToken() == Token.DOT) {
			processLexeme(Token.DOT);
			processLexeme(Token.IDENTIFIER);
		} // end while

		printIndented("Exit <qualified_identifier>", -1);
	} // end qualifiedIdentifier()

	// <program> = ["package" <qualified_identifier>] ";" {<import>} <class>;
	private void program() throws InvalidInputException {
		printIndented("Enter <program>", 1);

		if (nextLexeme.getToken() == Token.KEYWORD_PACKAGE) {
			processLexeme(Token.KEYWORD_PACKAGE);
			qualifiedIdentifier(); // <qualified_identifier>
			processLexeme(Token.SEMICOLON);
		} // end if

		while (nextLexeme.getToken() == Token.KEYWORD_IMPORT) {
			importRule(); // <import>
		}

		classRule(); // <class>

		printIndented("Exit <program>", -1);
	} // end program()

	// <import> = "import" ["static"] <identifier> {"." <identifier>} [".*"] ";" ;
	private void importRule() throws InvalidInputException {
		printIndented("Enter <import>", 1);

		processLexeme(Token.KEYWORD_IMPORT);
		// must check lexeme itself as "static" is a modifier
		if (nextLexeme.getLexeme().equals("static")) {
			processLexeme(Token.MODIFIER);
		}

		processLexeme(Token.IDENTIFIER);

		while (nextLexeme.getToken() == Token.DOT) {
			processLexeme(Token.DOT);
			if (nextLexeme.getLexeme().equals("*")) {
				// asterisk is an infix operator
				processLexeme(Token.INFIX_OPERATOR);
				break;
			} else {
				processLexeme(Token.IDENTIFIER);
			} // end if/else
		} // end while

		processLexeme(Token.SEMICOLON);

		printIndented("Exit <import>", -1);
	} // end importRule()

	// <class> = {<modifier>} <class declaration>;
	private void classRule() throws InvalidInputException {
		printIndented("Enter <class>", 1);

		while (nextLexeme.getToken() == Token.MODIFIER) {
			processLexeme(Token.MODIFIER);
		} // end while

		classDeclaration(); // <class_declaration>

		printIndented("Exit <class>", -1);
	} // end classRule()

	// <class_declaration> = "class" <identifier> [<type_arguments>][<extends>]
	// [<implements>] <class_body>;
	private void classDeclaration() throws InvalidInputException {
		printIndented("Enter <class_declaration>", 1);

		processLexeme(Token.KEYWORD_CLASS);
		processLexeme(Token.IDENTIFIER);

		if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET)
			typeArguments(); // <type_arguments>
		if (nextLexeme.getToken() == Token.KEYWORD_EXTENDS)
			extendsRule(); // <extends>
		if (nextLexeme.getToken() == Token.KEYWORD_IMPLEMENTS)
			implementsRule(); // <implements>

		classBody(); // <class_body>

		printIndented("Exit <class_declaration>", -1);
	}

	// <extends> = "extends" <identifier>;
	private void extendsRule() throws InvalidInputException {
		printIndented("Enter <extends>", 1);

		processLexeme(Token.KEYWORD_EXTENDS);
		processLexeme(Token.IDENTIFIER);

		printIndented("Exit <extends>", -1);
	} // end extendsRule()

	// <implements> = "implements" <identifier> {',' <identifier>};
	private void implementsRule() throws InvalidInputException {
		printIndented("Enter <implements>", 1);

		processLexeme(Token.KEYWORD_IMPLEMENTS);
		processLexeme(Token.IDENTIFIER);

		while (nextLexeme.getToken() == Token.COMMA) {
			processLexeme(Token.COMMA);
			processLexeme(Token.IDENTIFIER);
		} // end while

		printIndented("Exit <implements>", -1);
	} // end implementsRule()

	// <class_body> = '{' {<class body statement>} '}';
	private void classBody() throws InvalidInputException {
		printIndented("Enter <class_body>", 1);

		processLexeme(Token.LEFT_BRACE);

		while (nextLexeme.getToken() != Token.RIGHT_BRACE) {
			classBodyStatement();
		}

		processLexeme(Token.RIGHT_BRACE);

		printIndented("Exit <class_body>", -1);
	} // end <class_body>

	// <class_body_statement> = ';'
	//   | ["static"] <block>;
	//   | {<modifier>} <class_body_declaration>
	private void classBodyStatement() throws InvalidInputException {
		printIndented("Enter <class_body_statement>", 1);

		switch (nextLexeme.getToken()) {

		// semicolon: this is an empty statement
		case SEMICOLON:
			processLexeme(Token.SEMICOLON);
			break;

		// left brace: this is the opening of a block
		case LEFT_BRACE:
			block(); // <block>
			break;

		// otherwise, assume this is a declaration
		default:
			while (nextLexeme.getToken() == Token.MODIFIER) {
				processLexeme(Token.MODIFIER);
			} // end while

			if (nextLexeme.getToken() == Token.LEFT_BRACE) {
				block(); // <block>
			} else {
				classBodyDeclaration(); // <class_body_declaration>
			} // end if/else

		} // end switch

		printIndented("Exit <class_body_statement>", -1);
	} // end classBodyStatement()

	// <class_body_declaration> = <class_declaration>
	//    | "void" <identifier> <method_declaration>
	//    | <identifier> <method_declaration>
	//    | <type> <identifier> <method_declaration>
	//    | <type> <identifier> <field_declaration> ";";
	private void classBodyDeclaration() throws InvalidInputException {
		printIndented("Enter <class_body_declaration>", 1);

		switch (nextLexeme.getToken()) {

		// "class": this is a class declaration
		case KEYWORD_CLASS:
			classDeclaration(); // <class_declaration>
			break;

		// "void": this is a void method declaration
		case KEYWORD_VOID:
			processLexeme(Token.KEYWORD_VOID);
			processLexeme(Token.IDENTIFIER);
			methodDeclaration(); // <method_declaration>
			break;

		// identifiers and primitive types are handled together
		// as they could both by types
		case IDENTIFIER:
		case PRIMITIVE_TYPE:
			processLexeme(nextLexeme.getToken());
			
			if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET) {
				typeArguments();
			}
			
			if (nextLexeme.getToken() == Token.LEFT_PAREN) {
				// if immediately followed by a left paren,
				// this must be a constructor delcaration
				methodDeclaration(); // <method_declaration>
			}  else { // otherwise, we know that this is a type
				// {"[]"}
				while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
					processLexeme(Token.LEFT_BRACKET);
					processLexeme(Token.RIGHT_BRACKET);
				} // end while

				processLexeme(Token.IDENTIFIER);

				if (nextLexeme.getToken() == Token.LEFT_PAREN) {
					// if identifier followed by left paren,
					// this is a method declaration
					methodDeclaration(); // <method_declaration>
				} else {
					// if it isn't a left paren, this is a field declaration
					fieldDeclaration(); // <field_declaration>
					processLexeme(Token.SEMICOLON);
				}
			}

			break;

		default:
			error();

		} // end switch

		printIndented("Exit <class_body_declaration>", -1);
	} // end classBodyDeclaration()

	// <field_declaration> = {"[]"} ["=" <variable_init>] <variable_declarators_half>;
	private void fieldDeclaration() throws InvalidInputException {
		printIndented("Enter <field_declaration>", 1);

		while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
			processLexeme(Token.LEFT_BRACKET);
			processLexeme(Token.RIGHT_BRACKET);
		}

		if (nextLexeme.getToken() == Token.ASSIGNMENT_OPERATOR) {
			processLexeme(Token.ASSIGNMENT_OPERATOR);
			variableInit(); // <variable_init>
		}

		variableDeclaratorsHalf(); // <variable_declarators_half>

		printIndented("Exit <field_declaration>", -1);
	} // end fieldDeclaration()

	// <variable_declarator> = <identifier> {'[]'} ["=" <variable_init>];
	private void variableDeclarator() throws InvalidInputException {
		printIndented("Enter <variable_declarator>", 1);

		processLexeme(Token.IDENTIFIER);

		while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
			processLexeme(Token.LEFT_BRACKET);
			processLexeme(Token.RIGHT_BRACKET);
		}

		if (nextLexeme.getToken() == Token.ASSIGNMENT_OPERATOR) {
			processLexeme(Token.ASSIGNMENT_OPERATOR);
			variableInit(); // <variable_init>
		}

		printIndented("Exit <variable_declarators>", -1);
	} // end variableDeclarator()

	// <variable_declarators> = <variable_declarator> <variable_declarators_half>;
	private void variableDeclarators() throws InvalidInputException {
		printIndented("Enter <variable_declarators>", 1);

		variableDeclarator(); // <variable_declarator>

		variableDeclaratorsHalf(); // <variable_declarators_half>

		printIndented("Exit <variable_declarators>", -1);
	} // end variableDeclarators()

	// <variable_declarators_half> = {"," <variable_declarator>};
	private void variableDeclaratorsHalf() throws InvalidInputException {
		printIndented("Enter <variable_declarators_half>", 1);

		while (nextLexeme.getToken() == Token.COMMA) {
			processLexeme(Token.COMMA);
			variableDeclarator(); // <variable_declarator>
		}

		printIndented("Exit <variable_declarators_half>", -1);
	}

	// <method_declaration> = <parameters>
	// ["throws" <qualified_identifier> {"," <qualified_identifier>}]
	private void methodDeclaration() throws InvalidInputException {
		printIndented("Enter <method_declaration>", 1);

		parameters(); // <parameters>

		if (nextLexeme.getToken() == Token.KEYWORD_THROWS) {
			processLexeme(Token.KEYWORD_THROWS);
			qualifiedIdentifier(); // <qualified_identifier>

			while (nextLexeme.getToken() == Token.COMMA) {
				processLexeme(Token.COMMA);
				qualifiedIdentifier(); // <qualified_identifier>
			} // end while

		} // end if

		if (nextLexeme.getToken() == Token.SEMICOLON) {
			processLexeme(Token.SEMICOLON); // empty statement
		} else  {
			block(); // <block>
		}

		printIndented("Exit <method_declaration>", -1);
	}

	// <parameters> = "(" [<parameter> {, <parameter>}] ")";
	private void parameters() throws InvalidInputException {
		printIndented("Enter <parameters>", 1);

		processLexeme(Token.LEFT_PAREN);

		if (nextLexeme.getToken() != Token.RIGHT_PAREN) {

			parameter(); // <parameter>
			while (nextLexeme.getToken() == Token.COMMA) {
				processLexeme(Token.COMMA);
				parameter(); // <parameter>
			} // end while

		} // end if

		processLexeme(Token.RIGHT_PAREN);

		printIndented("Exit <parameters>", -1);
	}

	// <parameter> = {<modifier>} <type> <identifier>{"[]"};
	private void parameter() throws InvalidInputException {
		printIndented("Enter <parameter>", 1);

		while (nextLexeme.getToken() == Token.MODIFIER) {
			processLexeme(Token.MODIFIER);
		}

		type(); // <type>

		processLexeme(Token.IDENTIFIER);

		while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
			processLexeme(Token.LEFT_BRACKET);
			processLexeme(Token.RIGHT_BRACKET);
		}

		printIndented("Exit <parameter>", -1);
	} // end parameter()

	// <block> = '{' {<block_statement> }"}";
	private void block() throws InvalidInputException {
		printIndented("Enter <block>", 1);

		processLexeme(Token.LEFT_BRACE);

		while (nextLexeme.getToken() != Token.RIGHT_BRACE) {
			blockStatement(); // <block_statement>
		}

		processLexeme(Token.RIGHT_BRACE);

		printIndented("Exit <block>", -1);
	} // end block()

	// <block_statement> = {<modifier>} (
	// <class_declaration>
	//   | <local_variable_declaration>
	//   | <identifier> ":" <statement>
	//   | <identifier> [<type_arguments>] {"." <identifier> [<type_arguments>]} {"[]"} <variable_declarators>
	//   | <identifier> {"." <identifier>} <expression_from_block>
	private void blockStatement() throws InvalidInputException {
		printIndented("Enter <block_statement>", 1);

		// if the first lexeme is "synchronized," this is a synchronized block
		// which is handled in statement();
		if (nextLexeme.getLexeme().equals("synchronized")) {
			statement(); // <statement>

			// exit the method
			printIndented("Exit <block_statement>", -1);

			return;
		} // end if

		// cycle through all modifiers
		while (nextLexeme.getToken() == Token.MODIFIER) {
			processLexeme(Token.MODIFIER);
		}

		switch (nextLexeme.getToken()) {
		case KEYWORD_CLASS:
			classDeclaration(); // <class_declaration>
			break;

		case PRIMITIVE_TYPE:
			localVariableDeclaration(); // <local_variable_declaration>
			break;

		// identifier could either be a type or the start of an expression
		case IDENTIFIER:
			boolean typeArguments = false; // boolean to detect syntax errors

			processLexeme(Token.IDENTIFIER);

			// if the first token following the identifier is an
			// assignment operator, this is an expression
			if (nextLexeme.getToken() == Token.ASSIGNMENT_OPERATOR
					|| nextLexeme.getToken() == Token.OPERATOR_INCREMENT
					|| nextLexeme.getToken() == Token.OPERATOR_DECREMENT) {
				expressionFromBlock(); // <expression_from_block>
				break;
			}

			// if the next token is a colon, this is a statement following a label
			if (nextLexeme.getToken() == Token.COLON) {
				processLexeme(Token.COLON);
				statement(); // <statement>
			} else {
				if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET) {
					typeArguments(); // <type_arguments>
					typeArguments = true;
				} // end if
				while (nextLexeme.getToken() == Token.DOT) {
					processLexeme(Token.DOT);
					if (nextLexeme.getToken() != Token.IDENTIFIER) {
						//EXPRESSION: IDENTIFIER SUFFIX STARTING WITH DOT
						if (typeArguments)
							error(); // if any type arguments have occurred before this, error
						expressionFromBlock(); // <expression_from_block>
						break;
					} // end if
					processLexeme(Token.IDENTIFIER);
					if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET) {
						typeArguments(); // <type_arguments>
						typeArguments = true;
					} // end if
				} // end while

				if (nextLexeme.getToken() == Token.LEFT_PAREN) {
					// EXPRESSION: IDENTIFIER SUFFIX STARTING WITH LEFT PAREN
					if (typeArguments) error();
					expressionFromBlock();
					break;
				}
				while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
					processLexeme(Token.LEFT_BRACKET);
					processLexeme(Token.RIGHT_BRACKET);
				} // end while
				variableDeclarators(); // <variable_declarators>
			}
			break;

		default:
			statement(); // <statement>
			break;

		} // end switch

		printIndented("Exit <block_statement>", -1);
	}

	// this needs to be finished
	private void expressionFromBlock() throws InvalidInputException {
		printIndented("Enter <expression_from_block>", 1);

		if (nextLexeme.getToken() == Token.LEFT_PAREN) {
			arguments();
		} else if (nextLexeme.getToken() == Token.DOT) {
			// etc
		}

		if (nextLexeme.getToken() == Token.OPERATOR_INCREMENT || nextLexeme.getToken() == Token.OPERATOR_DECREMENT) {
			postfixOperator();
		}

		if (nextLexeme.getToken() == Token.ASSIGNMENT_OPERATOR
			|| nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET
			|| nextLexeme.getToken() == Token.RIGHT_ANGLEBRACKET
		) {
			assignmentOperator();
			expression1();
		}

		processLexeme(Token.SEMICOLON);

		printIndented("Exit <expression_from_block>", -1);
	} // end expressionFromBlock()

	// <local_variable_declaration> = <type> <variable_declarators>;
	private void localVariableDeclaration() throws InvalidInputException {
		printIndented("Enter <local_variable_declaration>", 1);

		type(); // <type>
		variableDeclarators(); // <variable_declarators>

		printIndented("Exit <local_variable_declaration>", -1);
	} // end <local_variable_declaration>

	// <type> = <primitive_type> {"[]"}
	//   | <identifier> <type_half>;
	private void type() throws InvalidInputException {
		printIndented("Enter <type>", 1);

		if (nextLexeme.getToken() == Token.PRIMITIVE_TYPE) {
			processLexeme(Token.PRIMITIVE_TYPE);
			while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
				processLexeme(Token.LEFT_BRACKET);
				processLexeme(Token.RIGHT_BRACKET);
			} // end while
		} else {
			processLexeme(Token.IDENTIFIER);
			typeHalf(); // <type_half>
		} // end else

		printIndented("Exit <type>", -1);
	} // end type()

	// <type_half> = [<type_arguments>] {"." <identifier> [type_arguments]}  {"[]"};
	private void typeHalf() throws InvalidInputException {
		printIndented("Enter <type_half>", 1);

		if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET)
			typeArguments(); // <type_arguments>

		while (nextLexeme.getToken() == Token.DOT) {
			processLexeme(Token.DOT);
			processLexeme(Token.IDENTIFIER);
			if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET)
				typeArguments(); // <type_arguments>
		} // end while

		while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
			processLexeme(Token.LEFT_BRACKET);
			processLexeme(Token.RIGHT_BRACKET);
		} // end while

		printIndented("Exit <type_half>", -1);
	} // end typeHalf()

	// <type_arguments> = "<" <type_argument> {"," <type_argument>} ">";
	private void typeArguments() throws InvalidInputException {
		printIndented("Enter <type_arguments>", 1);

		processLexeme(Token.LEFT_ANGLEBRACKET);

		typeArgument(); // <type_argument>

		while (nextLexeme.getToken() == Token.COMMA) {
			processLexeme(Token.COMMA);
			typeArgument(); // <type_argument>
		}

		processLexeme(Token.RIGHT_ANGLEBRACKET);

		printIndented("Exit <type_arguments>", -1);
	} // end typeArguments()

	// <type_argument> = <type> | "?" [ ("super" | "extends") <type>];
	private void typeArgument() throws InvalidInputException {
		printIndented("Enter <type_argument>", 1);

		if (nextLexeme.getToken() == Token.QUESTION_MARK) {
			processLexeme(Token.QUESTION_MARK);
			if (nextLexeme.getToken() == Token.KEYWORD_SUPER || nextLexeme.getToken() == Token.KEYWORD_EXTENDS) {
				processLexeme(nextLexeme.getToken());
				type(); // <type>
			}
		} else {
			type(); // <type>
		}

		printIndented("Exit <type_argument>", -1);
	} // end typeArgument()

	// <variable_init> = <expression> | <array_init>;
	private void variableInit() throws InvalidInputException {
		printIndented("Enter <variable_init>", 1);

		if (nextLexeme.getToken() == Token.LEFT_BRACE) {
			arrayInit(); // <array_init>
		} else {
			expression(); // <expression>
		}

		printIndented("Exit <variable_init>", -1);
	} // end variableInit()

	// <array_init> = "{" [<variable_init> {"," <variable_init>}] "}";
	private void arrayInit() throws InvalidInputException {
		printIndented("Enter <array_init>", 1);

		processLexeme(Token.LEFT_BRACE);

		if (nextLexeme.getToken() != Token.RIGHT_BRACE) {
			variableInit(); // <variable_init>
			while(nextLexeme.getToken() != Token.RIGHT_BRACE) {
				processLexeme(Token.COMMA);
				variableInit(); // <variable_init>
			} // end while
		} // end if

		processLexeme(Token.RIGHT_BRACE);

		printIndented("Exit <array_init>", -1);
	} // end array_init

	// <statement> = "if" <paren_expression> ["else" <statement>]
	//    | "while" <paren_expression> <statement>
	//    | "do" <statement> "while" <paren_expression> ";"
	//    | "for" "(" [{<modifier>} <type> <identifier> {"[]"}] ["=" <variable_init>] <for_arguments> ")" <statement>
	//    | "assert" <expression> [:<expression>] ";"
	//    | "switch" <paren_expression> "{" <cases> "}"
	//    | "return" [<expression>] ";"
	//    | "break" [<identifier>] ";"
	//    | "continue" [<identifier>] ";"
	//    | "throw" <expression> ";"
	//    | "try" <block> [<catches>] ["finally" <block>]
	//    | "synchronized" <paren_expression> <block>
	//    | <block>
	//    | ";"
	//    | <identifier> ":" <statement>
	//    | <identifier> <expression_half> ";"
	//    | <expression> ";" ;
	private void statement() throws InvalidInputException {
		printIndented("Enter <statement>", 1);

		switch (nextLexeme.getToken()) {

		// "if" <paren_expression> ["else" <statement>]
		case KEYWORD_IF:
			processLexeme(Token.KEYWORD_IF);
			parenExpression(); // <paren_expression>
			statement(); // <statement>
			if (nextLexeme.getToken() == Token.KEYWORD_ELSE) {
				processLexeme(Token.KEYWORD_ELSE);
				statement(); // <statement>
			} // end if
			break;

		// "while" <paren_expression> <statement>
		case KEYWORD_WHILE:
			processLexeme(Token.KEYWORD_WHILE);
			parenExpression(); // <paren_expression>
			statement(); // <statement>
			break;

		// "do" <statement> "while" <paren_expression> ";"
		case KEYWORD_DO:
			processLexeme(Token.KEYWORD_DO);
			statement(); // <statement>
			processLexeme(Token.KEYWORD_WHILE);
			parenExpression(); // <paren_expression>
			processLexeme(Token.SEMICOLON);
			break;

		// this is a bit messy

		//"for" "(" [{<modifier>} <type> <identifier> {"[]"}]
		// ["=" <variable_init>] <for_arguments> ")" <statement>
		case KEYWORD_FOR:
			processLexeme(Token.KEYWORD_FOR);
			processLexeme(Token.LEFT_PAREN);
			if (nextLexeme.getToken() != Token.COLON && nextLexeme.getToken() != Token.SEMICOLON) {
				while (nextLexeme.getToken() == Token.MODIFIER)
					processLexeme(Token.MODIFIER);
				type(); // <type>
				processLexeme(Token.IDENTIFIER);
				while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
					processLexeme(Token.LEFT_BRACKET);
					processLexeme(Token.RIGHT_BRACKET);
				}

				if (nextLexeme.getToken() == Token.ASSIGNMENT_OPERATOR
						|| nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET
						|| nextLexeme.getToken() == Token.RIGHT_ANGLEBRACKET
					) {
					assignmentOperator(); // <assignment_operator>
					variableInit(); // <variable_init>
				}
			}
			forArguments(); // <for_arguments>
			processLexeme(Token.RIGHT_PAREN);
			statement(); // <statement>
			break;

		// "switch" <paren_expression> "{" <cases> "}"
		case KEYWORD_SWITCH:
			processLexeme(Token.KEYWORD_SWITCH);
			parenExpression(); // <paren_expression>
			processLexeme(Token.LEFT_BRACE);
			cases(); // <cases>
			processLexeme(Token.RIGHT_BRACE);
			break;

		// "assert" <expression> [:<expression>] ";"
		case KEYWORD_ASSERT:
			processLexeme(Token.KEYWORD_ASSERT);
			expression(); // <expression>
			if (nextLexeme.getToken() == Token.COLON) {
				processLexeme(Token.COLON);
				expression(); // <expression>
			}
			processLexeme(Token.SEMICOLON);
			break;

		// "return" [<expression>] ";"
		case KEYWORD_RETURN:
			processLexeme(Token.KEYWORD_RETURN);
			if (nextLexeme.getToken() != Token.SEMICOLON) {
				expression(); // <expression>
			}
			processLexeme(Token.SEMICOLON);
			break;

		// "break" [<identifier>] ";"
		case KEYWORD_BREAK:
			processLexeme(Token.KEYWORD_BREAK);
			if (nextLexeme.getToken() == Token.IDENTIFIER)
				processLexeme(Token.IDENTIFIER);
			processLexeme(Token.SEMICOLON);
			break;

		// "continue" [<identifier>] ";"
		case KEYWORD_CONTINUE:
			processLexeme(Token.KEYWORD_CONTINUE);
			if (nextLexeme.getToken() == Token.IDENTIFIER)
				processLexeme(Token.IDENTIFIER);
			processLexeme(Token.SEMICOLON);
			break;

		// "throw" <expression> ";"
		case KEYWORD_THROW:
			processLexeme(Token.KEYWORD_THROW);
			expression(); // <expression>
			processLexeme(Token.SEMICOLON);
			break;

		// "try" <block> [<catches>] ["finally" <block>]
		case KEYWORD_TRY:
			processLexeme(Token.KEYWORD_TRY);
			block(); // <block>
			if (nextLexeme.getToken() == Token.KEYWORD_CATCH) {
				catches(); // <catches>
			}
			if (nextLexeme.getToken() == Token.KEYWORD_FINALLY) {
				processLexeme(Token.KEYWORD_FINALLY);
				block(); // <block>
			}
			break;

		// "synchronized" <paren_expression> <block>
		case MODIFIER:
			if (!nextLexeme.getLexeme().equals("synchronized"))
				error();
			processLexeme(Token.MODIFIER);
			parenExpression(); // <paren_expression>
			block(); // <block>
			break;

		// <block>
		case LEFT_BRACE:
			block();
			break;

		// ";"
		case SEMICOLON:
			processLexeme(Token.SEMICOLON);
			break;

		// <identifier> ":" <statement>
		// | <identifier> <expression_half>
		case IDENTIFIER:
			processLexeme(Token.IDENTIFIER);

			if (nextLexeme.getToken() != Token.COLON) {
				expressionHalf(); // <expression_half>
				processLexeme(Token.SEMICOLON);
			} else {
				processLexeme(Token.COLON);
				statement(); // <statement>
			}
			break;

		// <expression>
		default:
			expression(); // <expression>
			processLexeme(Token.SEMICOLON);
			break;

		}//end switch

		printIndented("Exit <statement>", -1);
	} // end statement()

	// <catches> = <catch> {<catch>};
	private void catches() throws InvalidInputException {
		printIndented("Enter <catches>", 1);

		while (nextLexeme.getToken() == Token.KEYWORD_CATCH) {
			catchRule(); // <catch>
		} // end while

		printIndented("Exit <catches>", -1);
	} // end catches()

	// <catch> = "catch" "(" {<modifier>} <qualified_identifier> <identifier> ")" <block>;
	private void catchRule() throws InvalidInputException {
		printIndented("Enter <catch>", 1);

		processLexeme(Token.KEYWORD_CATCH);
		processLexeme(Token.LEFT_PAREN);

		while (nextLexeme.getToken() == Token.MODIFIER) {
			processLexeme(Token.MODIFIER);
		} // end while

		qualifiedIdentifier(); // <qualified_identifier>
		processLexeme(Token.IDENTIFIER);
		processLexeme(Token.RIGHT_PAREN);
		block(); // <block>

		printIndented("Exit <catch>", -1);
	}

	// <for_arguments> = ";" [<expression>] ";" <expression> {"," <expression>}
	//    | ":" <expression>;
	private void forArguments() throws InvalidInputException {
		printIndented("Enter <for_arguments>", 1);

		if (nextLexeme.getToken() == Token.SEMICOLON) {
			processLexeme(Token.SEMICOLON);
			if (nextLexeme.getToken() != Token.SEMICOLON) expression();
			processLexeme(Token.SEMICOLON);
			if (nextLexeme.getToken() != Token.RIGHT_PAREN) {
				expression(); // <expression>
				while (nextLexeme.getToken() == Token.COMMA) {
					processLexeme(Token.COMMA);
					expression(); // <expression>
				} // end while
			} // end if
		} else {
			processLexeme(Token.COLON);
			expression(); // <expression>
		} // end else

		printIndented("Exit <for_arguments>", -1);
	} // end forArguments()

	// <cases> = { ("case" (<identifier> | <expression>) | "default") ":" {<block_statement>} };
	private void cases() throws InvalidInputException {
		printIndented("Enter <cases>", 1);

		while (nextLexeme.getToken() != Token.RIGHT_BRACE) {
			switch (nextLexeme.getToken()) {

			case KEYWORD_DEFAULT:
				processLexeme(Token.KEYWORD_DEFAULT);
				processLexeme(Token.COLON);
				break;

			case KEYWORD_CASE:
				processLexeme(Token.KEYWORD_CASE);
				if (nextLexeme.getToken() == Token.IDENTIFIER) {
					processLexeme(Token.IDENTIFIER);
				} else {
					expression(); // <expression>
				}
				processLexeme(Token.COLON);
				break;

			default:
				error();

			} // end switch

			while (nextLexeme.getToken() != Token.RIGHT_BRACE && nextLexeme.getToken() != Token.KEYWORD_CASE && nextLexeme.getToken() != Token.KEYWORD_DEFAULT) {
				blockStatement(); // <block_statement>
			}
		} // end cases

		printIndented("Exit <cases>", -1);
	} // end cases()

	//<expression> = <expression1> [<assignment_operator> <expression1>];
	private void expression() throws InvalidInputException {
		printIndented("Enter <expression>", 1);

		expression1(); // <expression1>
		
		if (nextLexeme.getToken() == Token.ASSIGNMENT_OPERATOR
				|| nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET
				|| nextLexeme.getToken() == Token.RIGHT_ANGLEBRACKET
			) {
			assignmentOperator(); // <assignment_operator>
			expression1(); // <expression1>
		}

		printIndented("Exit <expression>", -1);
	} // end expression()

	// <expression1> = <expression2> ["?" <expression> ":" <expression1>];
	private void expression1() throws InvalidInputException {
		printIndented("Enter <expression1>", 1);

		expression2(); // <expression2>
		
		if (nextLexeme.getToken() == Token.QUESTION_MARK) {
			processLexeme(Token.QUESTION_MARK);
			expression(); // <expression>
			processLexeme(Token.COLON);
			expression1(); // <expression>
		}

		printIndented("Exit <expression1>", -1);
	} // end expression1()

	private void expressionHalf() throws InvalidInputException {
		printIndented("Enter <expression_half>", 1);

		while (nextLexeme.getToken() == Token.DOT) {
			processLexeme(Token.DOT);
			if (nextLexeme.getToken() != Token.IDENTIFIER) break;
			processLexeme(Token.IDENTIFIER);
		}

		if (nextLexeme.getToken() == Token.LEFT_PAREN) {
			arguments();
		}

		if (nextLexeme.getToken() == Token.OPERATOR_INCREMENT || nextLexeme.getToken() == Token.OPERATOR_DECREMENT) {
			postfixOperator();
		}

		if (nextLexeme.getToken() == Token.ASSIGNMENT_OPERATOR
				|| nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET
				|| nextLexeme.getToken() == Token.RIGHT_ANGLEBRACKET
			) {
			assignmentOperator();
			expression1();
		}

		printIndented("Exit <expression_half>", -1);
	}

	// <expression2> = <expression3> [("instanceOf" <type> | {<infix_operator> <expression3>})];
	private void expression2() throws InvalidInputException {
		printIndented("Enter <expression2>", 1);

		expression3(); // <expression3>

		if (nextLexeme.getToken() == Token.KEYWORD_INSTANCEOF) {
			processLexeme(Token.KEYWORD_INSTANCEOF);
			type(); // <type>
		} else {
			while (nextLexeme.getToken() == Token.INFIX_OPERATOR
					|| nextLexeme.getToken() == Token.OPERATOR_PLUS
					|| nextLexeme.getToken() == Token.OPERATOR_MINUS
					|| nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET
					|| nextLexeme.getToken() == Token.RIGHT_ANGLEBRACKET ) {
				infixOperator(); // <infix_operator>
				expression3(); // <expression3>
			}
		}

		printIndented("Exit <expression2>", -1);
	} // expression2()

	// <expression3> = <prefix_operator> <expression3>
    //   | "(" <type> ")" <expression3> (*implemented ebnf *)
    //   | <expression_unit> {<selector>} {<postfix_operator>};
	private void expression3() throws InvalidInputException {
		printIndented("Enter <expression3>", 1);

		switch (nextLexeme.getToken()) {

		case PREFIX_OPERATOR:
		case OPERATOR_PLUS:
		case OPERATOR_MINUS:
		case OPERATOR_INCREMENT:
		case OPERATOR_DECREMENT:
			prefixOperator(); // <prefix_operator>
			expression3(); // <expression3>
			break;
			
		case LEFT_PAREN:
			processLexeme(Token.LEFT_PAREN);
			expression();
			processLexeme(Token.RIGHT_PAREN);
			break;

		default:
			expressionUnit(); // <expression_unit>
			while (nextLexeme.getToken() == Token.DOT) {
				selector(); // <selector>
			}
			while (nextLexeme.getToken() == Token.OPERATOR_INCREMENT || nextLexeme.getToken() == Token.OPERATOR_DECREMENT) {
				postfixOperator(); // <postfix_operator>
			}
			break;

		}

		printIndented("Exit <expression3>", -1);
	}

	private void expressionUnit() throws InvalidInputException {
		printIndented("Enter <expression_unit>", 1);

		switch (nextLexeme.getToken()) {

		case LEFT_PAREN:
			parenExpression();
			break;

		case KEYWORD_THIS:
			processLexeme(Token.KEYWORD_THIS);
			if (nextLexeme.getToken() == Token.LEFT_PAREN) arguments();
			break;

		case KEYWORD_SUPER:
			processLexeme(Token.KEYWORD_SUPER);
			if (nextLexeme.getToken() == Token.LEFT_PAREN) arguments();
			else {
				processLexeme(Token.DOT);
				processLexeme(Token.IDENTIFIER);
				if (nextLexeme.getToken() == Token.LEFT_PAREN) arguments();
			}
			break;

		case KEYWORD_NEW:
			processLexeme(Token.KEYWORD_NEW);
			allocator();
			break;

		case IDENTIFIER:
			processLexeme(Token.IDENTIFIER);
			identifierRest();
			break;

		case PRIMITIVE_TYPE:
			processLexeme(Token.PRIMITIVE_TYPE);
			while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
				processLexeme(Token.LEFT_BRACKET);
				processLexeme(Token.RIGHT_BRACKET);
			}
			processLexeme(Token.DOT);
			processLexeme(Token.KEYWORD_CLASS);
			break;

		case KEYWORD_VOID:
			processLexeme(Token.KEYWORD_VOID);
			processLexeme(Token.DOT);
			processLexeme(Token.KEYWORD_CLASS);
			break;

		default:
			literal();
			break;
		}

		printIndented("Exit <expression_unit>", -1);
	}

	private void identifierRest() throws InvalidInputException {
		printIndented("Enter <identifier_rest>", 1);

		while (nextLexeme.getToken() == Token.DOT) {
			processLexeme(Token.DOT);
			if (nextLexeme.getToken() != Token.IDENTIFIER) break;
			processLexeme(Token.IDENTIFIER);
		}

		if (nextLexeme.getToken() == Token.LEFT_PAREN) {
			arguments();
		}

		printIndented("Exit <identifier_rest>", -1);
	}

	//<arguments>
	private void arguments() throws InvalidInputException {
		printIndented("Enter <arguments>", 1);

		processLexeme(Token.LEFT_PAREN);

		if (nextLexeme.getToken() != Token.RIGHT_PAREN) {
			expression();
			while (nextLexeme.getToken() != Token.RIGHT_PAREN) {
				processLexeme(Token.COMMA);
				expression();
			}
		}

		processLexeme(Token.RIGHT_PAREN);

		printIndented("Exit <arguments>", -1);
	}

	// <paren_expression>
	private void parenExpression() throws InvalidInputException {
		printIndented("Enter <paren_expression>", 1);

		processLexeme(Token.LEFT_PAREN);
		expression();
		processLexeme(Token.RIGHT_PAREN);

		printIndented("Exit <paren_expression>", -1);
	}

	// <literal>
	private void literal() throws InvalidInputException {
		printIndented("Enter <literal>", 1);

		switch (nextLexeme.getToken()) {

		case INT_LITERAL:
			processLexeme(Token.INT_LITERAL);
			if (nextLexeme.getToken() == Token.DOT) {
				processLexeme(Token.DOT);
				processLexeme(Token.INT_LITERAL);
			}
			break;

		case CHAR_LITERAL:
			processLexeme(Token.CHAR_LITERAL);
			break;

		case STRING_LITERAL:
			processLexeme(Token.STRING_LITERAL);
			break;

		case KEYWORD_TRUE:
			processLexeme(Token.KEYWORD_TRUE);
			break;

		case KEYWORD_FALSE:
			processLexeme(Token.KEYWORD_FALSE);
			break;

		case KEYWORD_NULL:
			processLexeme(Token.KEYWORD_NULL);
			break;

		default:
			error();
		}

		printIndented("Exit <literal>", -1);
	}

	private void selector() throws InvalidInputException {
		printIndented("Enter <selector>", 1);

		processLexeme(Token.DOT);

		switch (nextLexeme.getToken()) {
		
		case IDENTIFIER:
			processLexeme(Token.IDENTIFIER);
			if (nextLexeme.getToken() == Token.LEFT_PAREN) arguments();
			break;

		case KEYWORD_THIS:
			processLexeme(Token.KEYWORD_THIS);
			break;

		case KEYWORD_SUPER:
			processLexeme(Token.KEYWORD_SUPER);
			if (nextLexeme.getToken() == Token.LEFT_PAREN) { 
				arguments(); // <arguments>
			} else {
				processLexeme(Token.DOT);
				processLexeme(Token.IDENTIFIER);
				if (nextLexeme.getToken() == Token.LEFT_PAREN) {
					arguments();
				} // end if
			} // end if/else
			break;

		case KEYWORD_NEW:
			processLexeme(Token.KEYWORD_NEW);
			// ??
			break;

		default:
			error();
		}

		printIndented("Exit <selector>", -1);
	}

	// <allocator> = <identifier> [<type_arguments>] {"." <identifier> [<type_arguments>]} (<class_allocator> | <array_allocator>);
	private void allocator() throws InvalidInputException {
		printIndented("Enter <allocator>", 1);
		
		processLexeme(Token.IDENTIFIER);
		
		if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET) {
			typeArguments(); // <type_arguments>
		}
		
		while (nextLexeme.getToken() == Token.DOT) {
			processLexeme(Token.DOT);
			processLexeme(Token.IDENTIFIER);
			
			if (nextLexeme.getToken() == Token.LEFT_ANGLEBRACKET) {
				typeArguments(); // <type_arguments>
			}
			
		} // end while
		
		if (nextLexeme.getToken() == Token.LEFT_PAREN) {
			classAllocator(); // <class_allocator>
		} else {
			arrayAllocator(); // <array_allocator>
		}
		
		printIndented("Exit <allocator>", -1);
	} // end allocator()
	
	// <class_allocator> = <arguments> [<class_body>];
	private void classAllocator() throws InvalidInputException{
		printIndented("Enter <class_allocator>", 1);
		
		arguments(); // <arguments>
		
		if (nextLexeme.getToken() == Token.LEFT_BRACE) {
			classBody(); // <class_body>
		}
		
		printIndented("Exit <class_allocator>", -1);
	} // end classAllocator()
	
	// <array_allocator> = "[]" {"[]"} <array_init>
    //   | "[" <expression> "]" {"[" <expression> "]"} {"[]"};
	private void arrayAllocator() throws InvalidInputException {
		printIndented("Enter <array_allocator>", 1);
		
		processLexeme(Token.LEFT_BRACKET);
		
		if (nextLexeme.getToken() == Token.RIGHT_BRACKET) {
			processLexeme(Token.RIGHT_BRACKET);
			
			while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
				processLexeme(Token.LEFT_BRACKET);
				processLexeme(Token.RIGHT_BRACKET);
			} // end while
			
			arrayInit(); // <array_init>
		} else {
			expression();
			processLexeme(Token.RIGHT_BRACKET);
			
			while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
				processLexeme(Token.LEFT_BRACKET);
				
				if (nextLexeme.getToken() == Token.RIGHT_BRACKET) {
					processLexeme(Token.RIGHT_BRACKET);
				} else {
					expression();
					processLexeme(Token.RIGHT_BRACKET);
				} // end if/ese
				
			} // end while
		
			while (nextLexeme.getToken() == Token.LEFT_BRACKET) {
				processLexeme(Token.LEFT_BRACKET);
				processLexeme(Token.RIGHT_BRACKET);
			} // end while
		} // end if/else
		
		printIndented("Exit <array_allocator>", -1);
	} // end arrayAllocator()
	
	
	// OPERATORS

	private void infixOperator() throws InvalidInputException {
		printIndented("Enter <infix_operator>", 1);

		switch (nextLexeme.getToken()) {
		case INFIX_OPERATOR:
		case OPERATOR_PLUS:
		case OPERATOR_MINUS:
			processLexeme(nextLexeme.getToken());
			break;

		case LEFT_ANGLEBRACKET:
			processLexeme(Token.LEFT_ANGLEBRACKET);

			switch (nextLexeme.getToken()) {
			case ASSIGNMENT_OPERATOR:
				processLexeme(Token.ASSIGNMENT_OPERATOR);
				break;
			case LEFT_ANGLEBRACKET:
				processLexeme(Token.LEFT_ANGLEBRACKET);
				break;
			default:
				break;
			} // end switch

			break;

		case RIGHT_ANGLEBRACKET:
			processLexeme(Token.RIGHT_ANGLEBRACKET);

			switch (nextLexeme.getToken()) {
			case ASSIGNMENT_OPERATOR:
				processLexeme(Token.ASSIGNMENT_OPERATOR);
				break;
			case RIGHT_ANGLEBRACKET:
				processLexeme(Token.RIGHT_ANGLEBRACKET);
				if (nextLexeme.getToken() == Token.RIGHT_ANGLEBRACKET)
					processLexeme(Token.RIGHT_ANGLEBRACKET);
				break;
			default:
				break;
			} // end switch

			break;

		default:
			error();
		} // end switch

		printIndented("Exit <infix_operator>", -1);
	}

	private void prefixOperator() throws InvalidInputException {
		printIndented("Enter <prefix_operator>", 1);

		switch (nextLexeme.getToken()) {
		case PREFIX_OPERATOR:
		case OPERATOR_PLUS:
		case OPERATOR_MINUS:
		case OPERATOR_INCREMENT:
		case OPERATOR_DECREMENT:
			processLexeme(nextLexeme.getToken());
			break;
		default:
			error();
		}

		printIndented("Exit <prefix_operator>", -1);
	}

	private void postfixOperator() throws InvalidInputException {
		printIndented("Enter <postfix_operator>", 1);

		switch (nextLexeme.getToken()) {
		case OPERATOR_INCREMENT:
		case OPERATOR_DECREMENT:
			processLexeme(nextLexeme.getToken());
			break;
		default:
			error();
		}

		printIndented("Exit <postfix_operator>", -1);
	}

	private void assignmentOperator() throws InvalidInputException {
		printIndented("Enter <assignment_operator>", 1);

		switch (nextLexeme.getToken()) {
		case ASSIGNMENT_OPERATOR:
			processLexeme(Token.ASSIGNMENT_OPERATOR);
			break;
		case LEFT_ANGLEBRACKET:
			processLexeme(Token.LEFT_ANGLEBRACKET);
			processLexeme(Token.LEFT_ANGLEBRACKET);
			processLexeme(Token.ASSIGNMENT_OPERATOR);
			break;
		case RIGHT_ANGLEBRACKET:
			processLexeme(Token.RIGHT_ANGLEBRACKET);
			processLexeme(Token.RIGHT_ANGLEBRACKET);
			if (nextLexeme.getToken() == Token.RIGHT_ANGLEBRACKET)
				processLexeme(Token.RIGHT_ANGLEBRACKET);
			processLexeme(Token.ASSIGNMENT_OPERATOR);
			break;
		default:
			error();
		}

		printIndented("Exit <assignment_operator>", -1);
	}

	/**
	* Checks if the current lexeme's associated token is equal to the given token,
	* prints out the current lexeme, and moves to the next lexeme in the input string
	*
	* @param token Expected token
	 * @throws InvalidInputException 
	*/
	private void processLexeme(Token token) throws InvalidInputException {
		if (nextLexeme.getToken() == token) {
			printIndented(nextLexeme.toString(), 0);
			nextLexeme = lex.nextLexeme();
		} else{
			error();
		}
	}

	//terminial printing version of printIndented
	/*
	private void printIndented(String toPrint) {
		for (int i = 0; i < indentationLevel; i++) {
			System.out.print("  ");
		}
		System.out.println(toPrint);
	}
	*/

	/**
	 * Modified print statement to maintain proper indentation
	 * @param toPrint String to print
	 * @param direction Positive integer to decrease indentation, negative to decrease,
	 *  or 0 to leave it the same.
	 */
	private void printIndented(String toPrint, int direction) {
		if (direction < 0) indentationLevel--;
		
		String output = lineNumber + ": ";
		lineNumber++;
		
		for (int i = 0; i < indentationLevel; i++) {
			output = output + "    ";
		}
		output = output + toPrint + "\n";
		
		outputQueue.add(output);
		if (direction > 0) indentationLevel++;
	}

	
	private void error() throws InvalidInputException {
		String message = String.format("ERROR: Line %d: Invalid input: %s\n", lex.getLineNumber(), nextLexeme.getLexeme());
		throw new InvalidInputException(message);
	}

	public ArrayDeque<String> getOutputQueue(){
		return outputQueue;
	}

} // end class
