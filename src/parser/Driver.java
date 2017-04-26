// old driver file

package parser;

import java.io.*;//needed for file support
import java.util.ArrayDeque;
import java.util.Scanner;//needed for Scanner

import types.InvalidInputException;

public class Driver {

	/**
	* This method converts a file to a single string.
	* as a bonus, it automatically gets rid of the newlines.
	* @param theFile The file to be converted
	* @return The string to be returned
	*/

	public static String fileToString(File theFile) throws IOException{
		String returnString = "";//the string to return
		Scanner fileReader = new Scanner(theFile);//set up a Scanner to read from the file
		while(fileReader.hasNext()){//while the file still has a next line
			returnString = returnString + fileReader.nextLine() + "\n";
		}
		fileReader.close();//close the file

		return returnString;
	}//end fileToString method

	public static void main (String args[]) throws IOException, InvalidInputException{
		
		//first get the file name, (as either command line arg, or user input;  I have included both ways)

		//command line version !!!!!!don't know if it works!!!!!
		//if(args.length != 2)//if the first argument doesn't exist
		//    System.exit(1);//error

		// File theFile = new File(args[1]);//make a File object with the filename of the argument
		//end command line version

		//user input version
		
		Scanner keyboard = new Scanner(System.in);//for keyboard input
		System.out.println("Please enter the name (or path) of the file to open.  ");
		String filename = keyboard.nextLine();//get input
		File aFile = new File(filename);//make a File object with the filename of the argument
		//end user input version

		keyboard.close();
		

		String inputString = fileToString(aFile);//convert the file to a single string
		
		/* debug code
		LexicalAnalyzer lex = new LexicalAnalyzer(inputString);
		
		for (int i = 0; i < 20; i++) {
			System.out.println(lex.nextLexeme());
		}
		
		System.exit(0);
		*/
		
		Parser parser = new Parser(inputString);
		parser.start();
		
		ArrayDeque<String> outputQueue = parser.getOutputQueue();
		
		while (!outputQueue.isEmpty()) {
			System.out.print(outputQueue.remove());
		}
		
	} //end of main

} //end of class
