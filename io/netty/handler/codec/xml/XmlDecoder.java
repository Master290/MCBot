package io.netty.handler.codec.xml;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;






















public class XmlDecoder
  extends ByteToMessageDecoder
{
  private static final AsyncXMLInputFactory XML_INPUT_FACTORY = new InputFactoryImpl();
  private static final XmlDocumentEnd XML_DOCUMENT_END = XmlDocumentEnd.INSTANCE;
  
  private final AsyncXMLStreamReader<AsyncByteArrayFeeder> streamReader = XML_INPUT_FACTORY.createAsyncForByteArray();
  private final AsyncByteArrayFeeder streamFeeder = (AsyncByteArrayFeeder)streamReader.getInputFeeder();
  
  public XmlDecoder() {}
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception { byte[] buffer = new byte[in.readableBytes()];
    in.readBytes(buffer);
    try {
      streamFeeder.feedInput(buffer, 0, buffer.length);
    } catch (XMLStreamException exception) {
      in.skipBytes(in.readableBytes());
      throw exception;
    }
    
    while (!streamFeeder.needMoreInput()) {
      int type = streamReader.next();
      switch (type) {
      case 7: 
        out.add(new XmlDocumentStart(streamReader.getEncoding(), streamReader.getVersion(), streamReader
          .isStandalone(), streamReader.getCharacterEncodingScheme()));
        break;
      case 8: 
        out.add(XML_DOCUMENT_END);
        break;
      
      case 1: 
        XmlElementStart elementStart = new XmlElementStart(streamReader.getLocalName(), streamReader.getName().getNamespaceURI(), streamReader.getPrefix());
        for (int x = 0; x < streamReader.getAttributeCount(); x++)
        {

          XmlAttribute attribute = new XmlAttribute(streamReader.getAttributeType(x), streamReader.getAttributeLocalName(x), streamReader.getAttributePrefix(x), streamReader.getAttributeNamespace(x), streamReader.getAttributeValue(x));
          elementStart.attributes().add(attribute);
        }
        for (int x = 0; x < streamReader.getNamespaceCount(); x++)
        {
          XmlNamespace namespace = new XmlNamespace(streamReader.getNamespacePrefix(x), streamReader.getNamespaceURI(x));
          elementStart.namespaces().add(namespace);
        }
        out.add(elementStart);
        break;
      
      case 2: 
        XmlElementEnd elementEnd = new XmlElementEnd(streamReader.getLocalName(), streamReader.getName().getNamespaceURI(), streamReader.getPrefix());
        for (int x = 0; x < streamReader.getNamespaceCount(); x++)
        {
          XmlNamespace namespace = new XmlNamespace(streamReader.getNamespacePrefix(x), streamReader.getNamespaceURI(x));
          elementEnd.namespaces().add(namespace);
        }
        out.add(elementEnd);
        break;
      case 3: 
        out.add(new XmlProcessingInstruction(streamReader.getPIData(), streamReader.getPITarget()));
        break;
      case 4: 
        out.add(new XmlCharacters(streamReader.getText()));
        break;
      case 5: 
        out.add(new XmlComment(streamReader.getText()));
        break;
      case 6: 
        out.add(new XmlSpace(streamReader.getText()));
        break;
      case 9: 
        out.add(new XmlEntityReference(streamReader.getLocalName(), streamReader.getText()));
        break;
      case 11: 
        out.add(new XmlDTD(streamReader.getText()));
        break;
      case 12: 
        out.add(new XmlCdata(streamReader.getText()));
      }
    }
  }
}
