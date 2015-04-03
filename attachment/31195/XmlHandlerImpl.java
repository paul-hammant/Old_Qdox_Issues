/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.jboss.jgettext.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.jdocbook.util.Constants;
import org.jboss.jgettext.Message;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A SAX event handler used to process DocBook XML into translatable entries.
 * <p/>
 * The processing logic is ported largely from the poxml tools from the kdesdk package (xml2pot and po2xml).  These
 * are the tools used by the RedHat documentation team with which we are interested in inter-operability.  Ideally one
 * would not mix usage of these "gettext" tools, but we have an explicit need to both execute these from Java and to
 * execute them on any operating system.  To the best of my knowledge, neither poxml nor kdesdk were available on
 * anything other than Linux.  As for the "from Java" bit, I was originally (in fact) just calling these tools
 * directly via system calls, which worked but given the previously discussed requirement and my in (in)experience with
 * skills necessary to port C++, Qt, and kdesdk code to cygwin I decided to port this functionality to Java.
 * <p/>
 * IMPLEMENTATION NOTE:	extending {@link DefaultHandler2} as we rely on certain SAX extension functionality.  Currently
 * this is limited to handling CDATA blocks ({@link org.xml.sax.ext.LexicalHandler}), but this may end up expanding to
 * deal with DTD entity declarations ({@link org.xml.sax.ext.DeclHandler}). 
 *
 * @author Steve Ebersole
 */
public class XmlHandlerImpl extends DefaultHandler2 {
	private static final Log log = LogFactory.getLog( XmlHandlerImpl.class );

	public static final List<String> SINGLE_TAGS = Arrays.asList(
			"beginpage","imagedata", "colspec", "spanspec", "anchor", "xref", "area", "footnoteref", "void",
			"inlinegraphic", "glosssee", "graphic", "xi:include"
	);

	public static final List<String> CUTTING_TAGS = Arrays.asList(
			"bridgehead", "trans_comment", "para", "title", "term", "entry", "contrib", "keyword", "example",
			"note", "footnote", "caution", "informalexample", "remark", "comment", "imageobject", "varlistentry",
			"thead", "tbody", "tgroup", "row", "screenshot", "screeninfo", "variablelist", "step", "procedure",
			"step", "holder", "listitem", "important", "author", "itemizedlist", "orderedlist", "caption",
			"textobject", "mediaobject", "tip", "glossdef", "inlinemediaobject", "simplelist", "member", "glossentry",
			"areaspec", "corpauthor", "indexterm", "calloutlist", "callout", "subtitle", "table", "part",
			"xi:fallback", "primary", "secondary", "chapter", "sect1", "sect2", "figure", "abstract", "sect3", "sect",
			"sect4", "warning", "preface", "authorgroup", "keywordset", "informaltable", "qandaentry", "question",
			"answer", "othercredit", "affiliation", "qandaset", "cmdsynopsis", "funcsynopsis", "funcsynopsisinfo" ,
			"epigraph", "attribution", "glossary", "chapterinfo", "glossdiv", "blockingquote", "simplesect", "section",
			"qandadiv", "refsect1", "refmeta", "formalpara", "refentry", "refnamediv", "refpurpose", "refentrytitle",
			"refmiscinfo", "refsect2", "refsect3", "refsect1info", "refsect2info", "refsect3info", "refsection",
			"refsectioninfo", "refsynopsisdiv", "refsysnopsisdivinfo", "remark", "revdescription", "glossentry",
			"partinfo", "segmentedlist", "segtitle", "seg", "seglistitem", "screenco"
	);

	public static final List<String> LITERAL_TAGS = Arrays.asList(
			"literallayout", "synopsis", "screen", "programlisting" 
	);

	public static final String LF = "&POXML_LINEFEED;";
	public static final String SPACE = "&POXML_SPACE;";
	public static final String LT = "&POXML_LT;";
	public static final String GT = "&POXML_GT;";
	public static final String AMP = "!POXML_AMP!";

	private final Nesting nesting = new Nesting();
	private int startline;
	private int startcol;

	private static Pattern positionsRegex = Pattern.compile( "\\s*poxml_line=\"(\\d+)\" poxml_col=\"(\\d+)\"" );
	private static Pattern doNotSplitRegex = Pattern.compile( "\\s*condition=\"do-not-split\"" );

	private ParaCounter paraCounter = new ParaCounter();
	private Locator locator;

	private StringBuilder msgidBuilder;
	private List<Message> list = new ArrayList<Message>();

	/**
	 * Constructs a new XmlHandlerImpl instance.
	 */
	public XmlHandlerImpl() {
		setDocumentLocator( new LocatorImpl() );
	}

	/**
	 * Getter for property 'messageList'.
	 *
	 * @return Value for property 'messageList'.
	 */
	public List<Message> getMessageList() {
		return list;
	}

	/**
	 * Getter for property 'paraCounter'.
	 *
	 * @return Value for property 'paraCounter'.
	 */
	public ParaCounter getParaCounter() {
		return paraCounter;
	}


	// SAX handler contracts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public void setDocumentLocator(Locator locator) {
		super.setDocumentLocator( locator );
		this.locator = locator;
	}

	/**
	 * {@inheritDoc}
	 */
	public void fatalError(SAXParseException e) throws SAXException {
		throw e;
	}

	/**
	 * {@inheritDoc}
	 */
	public void startDocument() throws SAXException {
		startline = 0;
		startcol = 0;
		nesting.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	public void endDocument() throws SAXException {
	}

	/**
	 * {@inheritDoc}
	 */
	public void skippedEntity(String name) throws SAXException {
		if ( nesting.isNested() ) {
			msgidBuilder.append( '&' ).append( name ).append( ';' );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void startCDATA() throws SAXException {
		if ( nesting.isNested() ) {
			msgidBuilder.append( "<![CDATA[" );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void endCDATA() throws SAXException {
		if ( nesting.isNested() ) {
			msgidBuilder.append( "]]>" );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void characters(char[] chars, int start, int length) {
		if ( nesting.isNested() && length > 0 ) {
			for ( int i = start; i < start + length; i++ ) {
				msgidBuilder.append( chars[i] );
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void comment(char ch[], int start, int length) throws SAXException {
		super.comment( ch, start, length );
	}

	/**
	 * {@inheritDoc}
	 */
	public void startElement(String uri, String name, String qName, Attributes atts) {
		String tname = qName.toLowerCase();
    	boolean first = false;

		if ( isCuttingTag( tname ) ) {
			if ( ! nesting.isNested() ) {
				msgidBuilder = new StringBuilder();
				paraCounter.increasePara();
				startline = locator.getLineNumber();
				startcol = locator.getColumnNumber();
				first = true;
			}
			nesting.increment();
    	}

		if ( nesting.isNested() ) {
			msgidBuilder.append( '<' ).append( tname );
			for ( int i = 0, X = atts.getLength(); i < X; i++ ) {
				msgidBuilder.append( ' ' )
						.append( atts.getQName( i ) )
						.append( "=\"" )
						.append( atts.getValue( i ) )
						.append( '\"' );
			}
			msgidBuilder.append( " poxml_line=\"" )
					.append( locator.getLineNumber() )
					.append( "\" poxml_col=\"" )
					.append( locator.getColumnNumber() )
					.append( '\"' );

			if ( isSingleTag( qName ) ) {
				msgidBuilder.append( "/>" );
			}
			else {
				msgidBuilder.append( '>' );
			}

			if ( first ) {
				startcol -= msgidBuilder.length();
			}
		}

		if ( "anchor".equals( tname ) || tname.startsWith( "sect" ) || "chapter".equals( tname ) ) {
			final String attrValue = atts.getValue( "id" );
			if ( attrValue != null && !"".equals( attrValue ) ) {
				paraCounter.addAnchor( attrValue );
			}
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public void endElement(String uri, String name, String qName) {
		String tname = qName.toLowerCase();
		if ( nesting.isNested() ) {
			if ( !isSingleTag( qName ) ) {
				msgidBuilder.append( "</" ).append( tname )
						.append( " poxml_line=\"" )
						.append( locator.getLineNumber() )
						.append( "\" poxml_col=\"" )
						.append( locator.getColumnNumber() )
						.append( "\">" );
	        }
    	}

		if ( isCuttingTag( tname ) ) {
			nesting.decrement();
			if ( !nesting.isNested() ) {
				Message m = new Message();
				m.setMsgid( descape( msgidBuilder ) );

				MessageParserState state = getMessageParserState( m );
				state.getBlockInfos().add(
						new BlockInfo(
								startline,
								startcol,
								locator.getLineNumber(),
								locator.getColumnNumber(),
								0
						)
				);

				formatMessage( m );

				List<Message> messages = splitMessage( m );
				for ( Message message : messages ) {
					// if the remaining text still starts with a tag, the poxml_ info
					// is most probably more correct
					if ( message.getMsgid().charAt( 0 ) == '<' && isClosure( message.getMsgid() ) ) {
						final Matcher matcher = positionsRegex.matcher( message.getMsgid() );
						if ( matcher.matches() ) {
							MessageParserState mstate = getMessageParserState( message );
							mstate.getBlockInfos().get( 0 ).startLine = Integer.parseInt( matcher.group( 1 ) );
							mstate.getBlockInfos().get( 0 ).startColumn = Integer.parseInt( matcher.group( 2 ) );
							mstate.getBlockInfos().get( 0 ).offset = 0;
						}
					}
					message.setMsgid( positionsRegex.matcher( message.getMsgid() ).replaceAll( "" ) );

					if ( message.getMsgid() != null && message.getMsgid().length() != 0 ) {
						list.add( message );
					}
				}
			}
		}
	}


	// Internal impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * A single, simplified entry point into the parsing of a DocBook XML file.
	 *
	 * @param xmlFile The DocBook XML file.
	 * @return The parsed Message list.
	 * @throws SAXException Indicates a problem processing the SAX events
	 * @throws IOException Indicates a problem reading the given DocBook XML file.
	 */
	public static List<Message> parseXML(File xmlFile) throws SAXException, IOException {
		StringBuilder contents = readIntoMemory( xmlFile );
		cleanupTags( contents );

		while ( true ) {
			int index = contents.indexOf( "<!ENTITY" );
			if ( index < 0 ) {
				break;
			}
			Nesting localNesting = new Nesting();
			int endindex = index + 1;
			String replacement = "";
			while ( contents.charAt( endindex ) != '>' || localNesting.isNested() ) {
				switch ( contents.charAt( endindex ) ) {
					case '<':
						localNesting.increment();
						break;
					case '>':
						localNesting.decrement();
						break;
					case '\n':
						replacement += '\n';
						break;
					default:
						break;
				}
				endindex++;
			}
			endindex++;
			contents.replace( index, endindex, replacement );
		}

		XMLReader reader = XMLReaderFactory.createXMLReader();

		XmlHandlerImpl handler = new XmlHandlerImpl();
		reader.setContentHandler( handler );
		reader.setErrorHandler( handler );
		// this next part is important to recieve notifications about CDATA blocks.  It is an optional SAX
		// feature, but the reader should throw exceptions here if it is not supported...
		reader.setProperty( "http://xml.org/sax/properties/lexical-handler", handler );

		// please dont kill performance trying to access DTD's over 'the net' ;)
		// just assume the user has validated DTD-compliance separately...
		reader.setFeature( Constants.DTD_LOADING_FEATURE, false );
		reader.setFeature( Constants.DTD_VALIDATION_FEATURE, false );

		InputSource source = new InputSource( new StringReader( contents.toString() ) );
		source.setSystemId( xmlFile.toURL().toString() );

		reader.parse( source );
		List<Message> english = handler.getMessageList();

		boolean changed;
		do {
			changed = false;
			Map<String, String> msgids = new HashMap<String,String>();

			for ( Message message : english ) {
				String tag = getMessageParserState( message ).getTag();
				if ( message.getMsgid().length() < 4 ) {
					message.setMsgid( '<' + tag + '>' + message.getMsgid() + "</" + tag + '>' );
					changed = true;
					break;
				}

				if ( msgids.containsKey( message.getMsgid() ) ) {
					String found = msgids.get( message.getMsgid() );
					if ( !found.equals( tag ) ) {
						changed = true;
						String msgid = message.getMsgid();
						for ( Message msg2 : english ) {
							if ( msg2.getMsgid().equals( msgid ) ) {
								msg2.setMsgid(  '<' + tag + '>' + msgid + "</" + tag + '>' );
							}
						}
						break;
					}
				}
				else {
					msgids.put( message.getMsgid(), tag );
				}
			}
		} while (changed);

		return english;
	}

	/**
	 * Read a file's contents into memory.
	 *
	 * @param xmlFile The file to read.
	 * @return The file contents.
	 * @throws IOException Indicates a problem reading the speficied file.
	 */
	public static StringBuilder readIntoMemory(File xmlFile) throws IOException {
		StringBuilder contents = new StringBuilder();
		FileReader file = new FileReader( xmlFile );
        final char[] buf = new char[ 1024 ];
        int length;
		while ( true ) {
			length = file.read( buf );
			if ( ( length <= 0 ) ) {
				break;
			}
			contents.append( buf, 0, length );
		}
		return contents;
	}

	public static MessageParserState getMessageParserState(Message message) {
		MessageParserState state = ( MessageParserState ) message.getParsingState().get( "XML_STATE" );
		if ( state == null ) {
			state = new MessageParserState();
			message.getParsingState().put( "XML_STATE", state );
		}
		return state;
	}

	public static boolean isCuttingTag(String qName) {
		return CUTTING_TAGS.contains( qName ) || isLiteralTag( qName );
	}

	public static boolean isSingleTag(String qName) {
		return SINGLE_TAGS.contains( qName );
	}

	public static boolean isLiteralTag(String qName) {
		return LITERAL_TAGS.contains( qName );
	}

	public static boolean isClosure(String message) {
	    assert( message.charAt( 0 ) == '<' );
    	int endindex = 1;
	    while ( !Character.isSpaceChar( message.charAt( endindex ) ) && message.charAt( endindex ) != '>' ) {
			endindex++;
		}
		return closureTag( message, message.substring( 1, endindex -1 ) );
	}

	public static boolean closureTag(CharSequence text, String tag) {
		if ( tag.startsWith( "![CDATA[") ) {
			return false;
		}
		Nesting localNesting = new Nesting();
		int index = 0;
		Matcher openMatcher = Pattern.compile( "<" + tag + "[>\\s]" ).matcher( text );
		Matcher closeMatcher = Pattern.compile( "</" + tag + "[>\\s]" ).matcher( text );
		while ( true ) {
			int nextclose = closeMatcher.find( index ) ? closeMatcher.start() : -1;
			if ( nextclose < 0 ) {
				return !localNesting.isNested() && index >= text.length();
			}

			int nextstart = openMatcher.find( index ) ? openMatcher.start() : -1;
			if ( nextstart < 0 ) {
				nextstart = text.length() + 1;
			}

			if ( nextstart < nextclose ) {
				localNesting.increment();
				index = nextstart + 1;
				while ( text.charAt( index ) != '>' ) {
					index++;
				}
				index++;
			}
			else {
				localNesting.decrement();
				index = nextclose + 1;
				while ( text.charAt( index ) != '>' ) {
					index++;
				}
				index++;
				if ( !localNesting.isNested() ) {
					return index >= text.length();
				}
			}
		}
	}

	public static String descape(StringBuilder text) {
		int index = 0;
		stripWhiteSpace( text );

		Nesting localNesting = new Nesting();
		boolean lastws = false;

		while ( index < text.length() ) {
			switch ( text.charAt( index ) ) {
				case '\n':
				case '\t':
				case '\r':
					if ( !localNesting.isNested() ) {
						text.replace( index, index + 1, " " );
					}
					// the original kdesdk code has no break statement here!!!
				case ' ':
					if ( !localNesting.isNested() && lastws ) {
						text.replace( index, index + 1, "\010" );
					}
					lastws = true;
					break;
				case '<': {
					int endindex = index+1;
					while ( endindex < text.length()
							&& !Character.isSpaceChar( text.charAt( endindex ) )
							&& text.charAt( endindex ) != '>' ) {
						endindex++;
					}
					String tag = text.substring( index + 1, endindex );
					if ( tag.length() >= 1 && tag.charAt( 0 ) == '/' ) {
						if ( isLiteralTag( tag.substring( 1 ) ) ) {
							localNesting.decrement();
						}
					}
					else {
						if ( isLiteralTag( tag ) ) {
							localNesting.increment();
						}
					}
					break;
				}
				default:
					lastws = false;
			}
			index++;
		}
		return Pattern.compile( "\010" ).matcher( text ).replaceAll( "" );
	}


	protected static boolean formatMessage(Message msg) {
		int offset = 0;
		boolean changed = false;
		boolean recurse = true;

		if ( msg.getMsgid() == null || msg.getMsgid().length() == 0 ) {
			return true;
		}

		StringBuilder localMsgidBuilder = new StringBuilder( msg.getMsgid() );
		for ( int index = 0; localMsgidBuilder.charAt( index ) == ' '; index++ ) {
			offset++;
		}
		stripWhiteSpace( localMsgidBuilder );

		// ugly goto-like-functionality hack...
		empty_string: do {

			// removing starting single tags
			for ( String singleTag : SINGLE_TAGS ) {
				if ( ( singleTag.length() + 1 ) < localMsgidBuilder.length()
						&& ( '<' + singleTag ).equals( localMsgidBuilder.substring( 0, singleTag.length() + 1 ) )
						&& !Character.isLetterOrDigit( localMsgidBuilder.charAt( singleTag.length() + 1 ) ) ) {
					int strindex = singleTag.length() + 1;
					while ( localMsgidBuilder.charAt( strindex ) != '>' ) {
						strindex++;
					}
					localMsgidBuilder.delete( 0, strindex + 1 );
					changed = true;
					offset += ( strindex + 1 );
					if ( localMsgidBuilder.length() == 0 ) {
						break empty_string;
					}
					for ( int index = 0; localMsgidBuilder.charAt( index ) == ' '; index++ ) {
						offset++;
					}
					stripWhiteSpace( localMsgidBuilder );
				}
			}

			if ( localMsgidBuilder.length() == 0 ) {
				break empty_string;
			}

			while ( localMsgidBuilder.length() > 2 && "/>".equals( localMsgidBuilder.substring( localMsgidBuilder.length() - 2 ) ) ) {
				int strindex = localMsgidBuilder.length() - 2;
				while ( localMsgidBuilder.charAt( strindex ) != '<' ) {
					strindex--;
				}
				localMsgidBuilder.delete( strindex, localMsgidBuilder.length() );
				stripWhiteSpace( localMsgidBuilder ); // only removed space at the end
				changed = true;
			}

			if ( localMsgidBuilder.length() == 0 ) {
				break empty_string;
			}

			for ( int index = 0; localMsgidBuilder.length() > 0 && localMsgidBuilder.charAt( index ) == ' '; index++ ) {
				offset++;
			}
			stripWhiteSpace( localMsgidBuilder );

			while ( true ) {
				if ( localMsgidBuilder.charAt( 0 ) != '<' ) {
					break;
				}
				if ( localMsgidBuilder.charAt( localMsgidBuilder.length() - 1 ) != '>' ) {
					break;
				}
	
				int strindex = 1;
				while ( localMsgidBuilder.charAt( strindex ) != ' ' && localMsgidBuilder.charAt( strindex ) != '>' ) {
					strindex++;
				}
				String starttag = localMsgidBuilder.substring( 1, strindex );

				int endindex = localMsgidBuilder.length() - 2;
				while ( localMsgidBuilder.charAt( endindex ) != '<' && localMsgidBuilder.charAt( endindex + 1 ) != '/' ) {
					endindex--;
				}

				String endtag = localMsgidBuilder.substring( endindex + 2, localMsgidBuilder.length() - 1 );
				int attrStart = endtag.indexOf( ' ' );
				String endtag_attr = attrStart < 0 ? "" : endtag.substring( attrStart );
				endtag = positionsRegex.matcher( endtag ).replaceAll( "" );
				if ( endtag.equals( starttag ) ) {
					if ( !closureTag( localMsgidBuilder, starttag ) ) {
						break;
					}

					// removing start/end tags
					localMsgidBuilder.delete( endindex, localMsgidBuilder.length() );
					strindex = 0;
					while ( localMsgidBuilder.charAt( strindex ) != '>' ) {
						strindex++;
					}
					String attr = localMsgidBuilder.substring( 0, strindex );
					localMsgidBuilder.delete( 0, strindex + 1 );
					offset += ( strindex + 1 );
					for ( int index = 0; localMsgidBuilder.charAt( index ) == ' '; index++ ) {
						offset++;
					}
					stripWhiteSpace( localMsgidBuilder );
					MessageParserState state = getMessageParserState( msg );
					state.setTag( starttag );

					Matcher attrMatcher = positionsRegex.matcher( attr );
					if ( attrMatcher.find() ) {
						state.getBlockInfos().get( 0 ).startLine = Integer.parseInt( attrMatcher.group( 1 ) );
						state.getBlockInfos().get( 0 ).startColumn = Integer.parseInt( attrMatcher.group( 2 ) );
						offset = 0;

						Matcher endtagAttrMatcher = positionsRegex.matcher( endtag_attr );
						if ( endtagAttrMatcher.find() ) {
							state.getBlockInfos().get( 0 ).endLine = Integer.parseInt( endtagAttrMatcher.group( 1 ) );
							state.getBlockInfos().get( 0 ).endColumn = Integer.parseInt( endtagAttrMatcher.group( 2 ) );
						}
					}

					if ( doNotSplitRegex.matcher( attr ).find() ) {
						state.setDisallowSplit( Boolean.TRUE );
						break;
					}

					changed = true;
				}
				else {
					break;
				}
			}

			break empty_string;

		} while ( true ); // <end> ugly goto-like-functionality hack...

		if ( changed ) {
			msg.setMsgid( localMsgidBuilder.toString() );
		}

		MessageParserState state = getMessageParserState( msg );
		state.getBlockInfos().get( 0 ).offset += offset;

		if ( state.getDisallowSplit() == Boolean.TRUE ) {
			recurse = false;
		}

		if ( changed && recurse ) {
			formatMessage( msg );
		}

		return !recurse; // indicates an abort
	}

	private static CharSequence loggableMsgid(CharSequence msgid, int length) {
		return loggableMsgid( msgid, length, Position.BOTH );
	}

	private static CharSequence loggableMsgid(CharSequence msgid, int length, Position position) {
		if ( position == Position.BOTH ) {
			return msgid.length() <= ( length * 2 )
					? msgid
					: msgid.subSequence( 0, length ) + "..." + msgid.subSequence( msgid.length() - length, msgid.length() );
		}
		else if ( position == Position.START ) {
			return msgid.length() <= length
					? msgid
					: msgid.subSequence( 0, length ) + "...";
		}
		else { // Position.END
			return msgid.length() <= length
					? msgid
					: "..." + msgid.subSequence( 0, length );
		}
	}


	// NOTE : the semi-colon is unnecessary here from the Java language perspective
	//		however, the qdox parser used by the maven-plugin-plugin expects it and so it
	//		is added here as a workaround:
	private static enum Position { START, END, BOTH; }

	protected static List<Message> splitMessage(Message mb) {
		List<Message> result = new ArrayList<Message>();

		if ( mb.getMsgid().length() == 0 ) {
			return result;
		}

		StringBuilder msgid = new StringBuilder( mb.getMsgid() );
		stripWhiteSpace( msgid );
		log.debug( "looking to split msgid [[[" + loggableMsgid( msgid, 40 ) + "]]]" );

		Message msg1 = mb.copy();
		msg1.setMsgid( msgid.toString() );
		Message msg2 = mb.copy();
		msg2.setMsgid( msgid.toString() );


		if ( msgid.charAt( 0 ) == '<' ) {
			int endindex = 1;
			while ( !Character.isSpaceChar( msgid.charAt( endindex ) ) && msgid.charAt( endindex ) != '>' ) {
				endindex++;
			}
			String tag = msgid.substring( 1, endindex );

        	if ( closureTag( msgid, tag ) ) {
				result.add( mb );
				return result;
			}

			if ( isCuttingTag( tag ) ) {
				// if the message starts with a cutting tag, this tag has to
				// end in between. We split both messages and format them
				int strindex = endindex;
				strindex++;

				Nesting localNesting = new Nesting();
				localNesting.increment();

				Matcher closingMatcher = Pattern.compile( "</" + tag + "[\\s>]" ).matcher( msgid );
				Matcher openingMatcher = Pattern.compile( "<" + tag + "[\\s>]" ).matcher( msgid );

				while ( true ) {
					int closing_index = closingMatcher.find( strindex ) ? closingMatcher.start() : -1;
					assert( closing_index != -1 );
					int starting_index = openingMatcher.find( strindex ) ? openingMatcher.start() : -1;


					// when a new start was found, we set the start_index after the next match
					// (and set strindex to it later - increasing inside)
					if ( starting_index != -1 ) {
						starting_index += ( tag.length() + 1 );
						while ( msgid.charAt( starting_index ) != '>' ) {
							starting_index++;
						}
						starting_index++;
					}

					closing_index += 3 + tag.length();
					while ( msgid.charAt( closing_index - 1 ) != '>' ) {
						closing_index++;
					}

					if ( starting_index < 0 ) {
						strindex = closing_index;
						localNesting.decrement();
						if ( !localNesting.isNested() ) {
							break;
						}
						continue;
					}
					if ( closing_index < starting_index )  {
						strindex = closing_index;
						localNesting.decrement();
					}
					else {
						strindex = starting_index;
						localNesting.increment();
					}

					if ( !localNesting.isNested() ) {
						break;
					}
				}

				msg1.setMsgid( msgid.substring( 0, strindex ) );
				log.debug( "msgid split from start #1 (0->" + strindex + ") :  [[[" + loggableMsgid( msg1.getMsgid(), 30, Position.START ) + "]]]" );
				boolean leave = formatMessage( msg1 );
				log.debug( "    after format :  [[[" + loggableMsgid( msg1.getMsgid(), 30, Position.START ) + "]]]" );

				msg2.setMsgid( msgid.substring( strindex ) );
				log.debug( "msgid split from start #2 (" + strindex + "->end) :  [[[" + loggableMsgid( msg2.getMsgid(), 30, Position.START ) + "]]]" );
				BlockInfo bi2 = getMessageParserState( msg2 ).getBlockInfos().get( 0 );
				bi2.offset += strindex;
				leave = leave & formatMessage( msg2 );
				log.debug( "    after format :  [[[" + loggableMsgid( msg2.getMsgid(), 30, Position.START ) + "]]]" );

				BlockInfo bi1 = getMessageParserState( msg1 ).getBlockInfos().get( 0 );
				if ( bi1.endLine > bi2.startLine
						|| ( bi1.endLine == bi2.startLine && bi1.endColumn > bi2.startColumn ) ) {
					bi2.startLine = bi1.endLine;
					bi2.startColumn = bi1.endColumn;
				}

				if ( leave ) {
					result.add( msg1 );
					result.add( msg2 );
					return result;
				}

				result.addAll( splitMessage( msg1 ) );
				result.addAll( splitMessage( msg2 ) );
				return result;
			}
		}

		if ( msgid.charAt( msgid.length() - 1 ) == '>' ) {
			int endindex = msgid.length() - 1;
			while ( endindex >= 0 && ( msgid.charAt( endindex ) != '<' || msgid.charAt( endindex + 1 ) != '/' ) ) {
				endindex--;
			}
			String tag = msgid.substring( endindex + 2, msgid.length() - 3 );
			int attrSplit = tag.indexOf( ' ' );
			if ( attrSplit > 0 ) {
				tag = tag.substring( 0, attrSplit );
			}
			log.trace( "splitting on tag [" + tag + "]" );

			if ( isCuttingTag( tag ) ) {
				// if the message ends with a cutting tag, this tag has to
				// start in between. We split both messages and format them
				int strindex = endindex;

				Nesting localNesting = new Nesting();
				localNesting.increment();

				Pattern closing = Pattern.compile( "</" + tag + "[\\s>]" );
				Pattern opening = Pattern.compile( "<" + tag + "[\\s>]" );

				while ( true ) {
					int openIndex = regexRev( msgid, opening, strindex - 1 );

					if ( openIndex == -1 ) {
						assert( localNesting.getDepth() == 1 );
						break;
					}

					int closeIndex = regexRev( msgid, closing, strindex - 1 );

					if ( closeIndex > openIndex ) {
						strindex = closeIndex;
						localNesting.increment();
					}
					else {
						strindex = openIndex;
						localNesting.decrement();
					}

					if ( !localNesting.isNested() ) {
						break;
					}
				}

				msg1.setMsgid( msgid.substring( 0, strindex ) );
				log.debug( "msgid split from end #1 (0->" + strindex + ") :  [[[" + loggableMsgid( msg2.getMsgid(), 30, Position.START ) + "]]]" );
				formatMessage( msg1 );

				msg2.setMsgid( msgid.substring( strindex ) );
				log.debug( "msgid split from end #2 (" + strindex + "->end) :  [[[" + loggableMsgid( msg2.getMsgid(), 30, Position.START ) + "]]]" );
				BlockInfo bi2 = getMessageParserState( msg2 ).getBlockInfos().get( 0 );
				bi2.offset += strindex;
				formatMessage( msg2 );

				BlockInfo bi1 = getMessageParserState( msg1 ).getBlockInfos().get( 0 );
				if ( bi1.endLine > bi2.startLine || ( bi1.endLine == bi2.startLine && bi1.endColumn > bi2.startColumn ) ) {
					bi1.endLine = bi2.startLine;
					bi1.endColumn = bi2.startColumn - 1;
				}

				result.addAll( splitMessage( msg1 ) );
				result.addAll( splitMessage( msg2 ) );
				return result;
			}
		}

		result.add( mb );
    	return result;
	}

	private static int regexRev(CharSequence text, Pattern pattern, int maxIndex) {
		int valid = -1;
		int current = 0;
		Matcher matcher = pattern.matcher( text );
		matcher.region( 0, maxIndex );
		while ( true ) {
			current = matcher.find( current + 1 ) ? matcher.start() : -1;
			if ( current < 0 ) {
				break;
			}
			else if ( current < maxIndex ) {
				valid = current;
			}
			else {
				break;
			}
		}
		return valid;
	}


	static boolean stripWhiteSpace(StringBuilder contents) {
		boolean changed;
		do {
			changed = false;

			int index = 0;
			while ( Character.isSpaceChar( contents.charAt( index ) ) ) {
				index++;
			}
			if ( index > 0 ) {
				contents.delete( 0, index );
				changed = true;
			}

			index = contents.length();
			while ( Character.isSpaceChar( contents.charAt( index - 1 ) ) ) {
				index--;
			}
			if ( index < contents.length() ) {
				contents.delete( index, contents.length() );
				changed = true;
			}

			if ( contents.indexOf( LF ) == 0 ) {
				contents.delete( 0, LF.length() );
				changed = true;
			}
			if ( contents.indexOf( SPACE ) == 0 ) {
				contents.delete( 0, SPACE.length() );
				changed = true;
			}
			if ( contents.lastIndexOf( LF ) == LF.length() ) {
				contents.delete( contents.length() - LF.length(), contents.length() );
				changed = true;
			}
			if ( contents.lastIndexOf( SPACE ) == SPACE.length() ) {
				contents.delete( contents.length() - SPACE.length(), contents.length() );
				changed = true;
			}
		} while ( changed );

		return changed;
	}

	public static void cleanupTags(StringBuilder contents) {
		log.trace( "cleaning up tags" );
		contents = new StringBuilder( Pattern.compile( "&" ).matcher( contents ).replaceAll( AMP ) );

		for ( String tag : LITERAL_TAGS ) {
			Matcher start = Pattern.compile( "<" + tag + "[\\s>]" ).matcher( contents );
			Matcher end = Pattern.compile( "</" + tag + "[\\s>]" ).matcher( contents );
			int strindex = 0;
			while ( true ) {
				if ( ! start.find( strindex ) ) {
					break;
				}
				strindex = start.start();
				while ( contents.charAt( strindex ) != '>' ) {
					strindex++;
				}
				strindex++; // one more

				end.find( strindex );
				int endindex = end.start();

				String part = contents.substring( strindex, endindex );
				String newpart = escapeLiterals( part );
				log.debug( "clean-up tag (literal) : " + tag );
				contents.replace( strindex, endindex, newpart );
				// this assumes that literal tags to not overlap
				strindex += newpart.length();
			}
		}

		Matcher unclosed = Pattern.compile( "</(\\w*)\\s\\s*>" ).matcher( contents );
		int index = -1;
		while ( unclosed.find( index + 1 ) ) {
			index = unclosed.start( 1 );
			String tag = unclosed.group( 1 );
			log.debug( "auto-closing tag : " + tag );
			contents.replace( index, unclosed.end( 1 ), "</" + tag + ">" );
		}

//		Matcher start = Pattern.compile( "<((\\s*[^<>\\s])*)\\s\\s*(/*)>" ).matcher( contents );
		Matcher start = Pattern.compile( "<((\\s*?[^<>\\s])*?)\\s\\s*?(/*?)>" ).matcher( contents );
		index = -1;
		while ( start.find( index + 1 ) ) {
			index = start.start( 1 );
			String tag = start.group( 1 );
			String cut = start.group( start.groupCount() );
			contents.replace( index, start.end( 1 ), '<' + tag + cut + '>' );
		}

		Matcher singletag = Pattern.compile( "<(\\w*)\\s([^><]*)/>" ).matcher( contents );
	    index = -1;
		while ( singletag.find( index + 1 ) ) {
			index = singletag.start( 1 );
			String tag = singletag.group( 1 );
			if ( !isSingleTag( tag ) ) {
				contents.replace( index, singletag.end(), '<' + tag + ' ' + singletag.group( 2 ) + "></" + tag + '>' );
			}
		}

		Matcher transComment = Pattern.compile( "<!-- TRANS:([^<>]*)-->" ).matcher( contents );
	    index = -1;
    	while ( transComment.find( index + 1 ) ) {
			index = transComment.start( 1 );
        	String msgid = transComment.group( 1 );
        	contents.replace( index, transComment.end( 1 ), "<trans_comment>" + msgid + "</trans_comment>" );
    	}
		log.trace( "done cleaning up tags" );
	}

	public static boolean removeEmptyTag(StringBuilder text, String tag) {
		Pattern empty = Pattern.compile( "<" + tag + "[^>]*>[\\s\n][\\s\n]*</" + tag + "\\s*>" );
		int strindex = 0;
		Matcher matcher;
		while ( ( matcher = empty.matcher( text ) ).find( strindex ) ) {
			strindex = matcher.start();
			log.debug( "found empty tag " + tag );
			text.replace( strindex, matcher.end(), " " );
			strindex++;
		}
		return strindex != 0;
	}

	public static void removeEmptyTags(StringBuilder text) {
		boolean removed;
		do {
			removed = false;

			for ( String tag : CUTTING_TAGS ) {
				if ( removeEmptyTag( text, tag ) ) {
					removed = true;
					break;
				}
			}

			// as glossterm has two different semantics, it's likely
			// to break something when it's cuttingtag
			if ( removeEmptyTag( text, "glossterm" ) ) {
				removed = true;
			}

		} while ( removed );
	}

	public static String escapeLiterals(String text) {
		return text.replaceAll( "\n", LF )
				.replaceAll( "<", LT )
				.replaceAll( ">", GT )
				.replaceAll( "\t", "        " )
				.replaceAll( " ", SPACE );
	}

	public static String descapeLiterals(String text) {
		return text.replaceAll( LF, "\n" )
				.replaceAll( LT, "<" )
				.replaceAll( GT, ">" )
				.replaceAll( SPACE, " " )
				.replaceAll( AMP, "&" );
	}

	public static class ParaCounter {
		private int current;
		private Map<String,Integer> anchors = new HashMap<String,Integer>();

	    void addAnchor(String anchor) {
			anchors.put( anchor, current );
		}

		void increasePara() {
			current++;
		}

		/**
		 * Getter for property 'anchors'.
		 *
		 * @return Value for property 'anchors'.
		 */
		public Map<String, Integer> getAnchors() {
			return anchors;
		}
	}

}
