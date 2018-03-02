package converter.fileConverters;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Token {

	public int begin;
	public int end;
	public int line;
	public int offsetLine;
	public String span;
	public static ArrayList<ArrayList<Token>> lines;
	public static ArrayList<Token> tokens;

	private static Hashtable<Integer,Token> tokenHash;
	
	private static Pattern lineBreaker = Pattern.compile("\\n(\\r)?");
	private static Pattern tokenBreaker = Pattern.compile("\\s+");
	
	public static void Tokenize(String text) throws Exception
	{
		tokenHash = null;
		Matcher matcherLineBreak = lineBreaker.matcher(text);
		
		//create first line
		lines = new ArrayList<ArrayList<Token>>();
		tokens = new ArrayList<Token>();
		ArrayList<Token> line = new ArrayList<Token>();
	
		int prevLineEnd = 0;
		int lineId = 0;
		int currTokenOffset = 0;
		
		int startBreak, endBreak;
		while (matcherLineBreak.find())
		{
			lineId++;
			
			startBreak = matcherLineBreak.start();
			endBreak = matcherLineBreak.end();
			
			int startLine = prevLineEnd;
			int endLine = startBreak;
			
			//create new line
			line = new ArrayList<Token>();
			currTokenOffset = 0;
			lines.add(line);
			
			String lineText = text.substring(startLine, endLine);
			//System.out.println("(" + lines.size() + ") " + lineText);
			
			if (lineText.length() > 0)
			{
				//System.out.println(lineText);
				
				//create first token annotation
				Token token = new Token();
				token.begin = startLine;
				token.end = endLine;
				token.line = lineId;
				token.offsetLine = currTokenOffset;
				tokens.add(token);
				line.add(token);
				Token prevToken = token;
				
				Matcher matcherWhitespace = tokenBreaker.matcher(lineText);
				while (matcherWhitespace.find())
				{
					currTokenOffset++;
					
					int startSpace = matcherWhitespace.start();
					int endSpace = matcherWhitespace.end();
					
					token = new Token();
					token.begin = startLine + endSpace;
					token.line = lineId;
					token.offsetLine = currTokenOffset;
					line.add(token);
					tokens.add(token);
					prevToken.end = startLine + startSpace;
					prevToken = token;
				}
				token.end = endLine;
			}
			
			prevLineEnd = endBreak;
			
			/*for (int t=tokens.size()-line.size(); t<tokens.size(); t++)
			{
				System.out.print(text.substring(tokens.get(t).begin,tokens.get(t).end) + "_");
			}
			System.out.println();*/
		}
		tokenHash = buildTokenHashTable();
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	private static Hashtable<Integer,Token> buildTokenHashTable() throws Exception
	{
		Hashtable<Integer,Token> hashtable = new Hashtable<Integer,Token>();
		
		//check if there are token annotations
		for (Token token: tokens)
		{
			for (int i=0; i < token.end-token.begin; i++)
			{
				hashtable.put(new Integer(token.begin + i), token);
			}
		}
		return hashtable;
	}
	
	/**
	 * 
	 * @param charOffset
	 * @param isStartingOffset
	 * @return
	 * @throws Exception
	 */
	public static Token getTokenFromCharOffset(int charOffset, boolean isStartingOffset) throws Exception
	{
		try
		{
			int shifting;
			
			if (isStartingOffset)
				 shifting =  1;
			else shifting = -1;
			
			Token tokenObj = tokenHash.get(new Integer(charOffset));
			
			while(tokenObj == null){
				charOffset += shifting;
				tokenObj = tokenHash.get(new Integer(charOffset));
			}
			return tokenObj;
		}
		catch(Exception e){
			throw new Exception("Token does not exist at offset " + charOffset + "(looked for token start = "+ isStartingOffset +")");
		}
	}
}
