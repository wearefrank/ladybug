package nl.nn.testtool.metadata;

import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.util.XmlUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.text.html.Option;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ExtractionStrategy {
    Optional<String> extract(String message);
}

class RegexExtractionStrategy implements ExtractionStrategy {
    private Pattern pattern;

    public RegexExtractionStrategy(String regex) {
        setRegex(regex);
    }

    public void setRegex(String regex) {
        pattern = (regex == null || regex.isEmpty()) ? null : Pattern.compile(regex);
    }

    @Override
    public Optional<String> extract(String message) {
        if (message == null || message.isEmpty() || pattern == null) {
            return Optional.empty();
        }

        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0));
        }
        return Optional.empty();
    }
}

class XpathExtractionStrategy implements ExtractionStrategy {
    private @Setter @Getter XPathExpression xpathExpression;

    public XpathExtractionStrategy(String xpath) throws XPathExpressionException {
        if (xpath != null) {
            setXpathExpression(XmlUtil.createXPathExpression(xpath));
        }
    }

    @Override
    public Optional<String> extract(String message) {
        try {
            return Optional.ofNullable(xpathExpression.evaluate(XmlUtil.createXmlSourceFromString(message)));
        } catch (XPathExpressionException e) {
            return Optional.empty();
        }
    }
}