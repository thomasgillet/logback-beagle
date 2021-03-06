package ch.qos.logback.beagle.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import ch.qos.logback.beagle.Activator;
import ch.qos.logback.beagle.preferences.BeaglePreferencesPage;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.CompositeConverter;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ScanException;

public class ConverterFacade {

  String pattern;
  private Converter<ILoggingEvent> head;
  Context context;
  List<Converter<ILoggingEvent>> converterList = new ArrayList<Converter<ILoggingEvent>>();

  public void start() {
    converterList.clear();
    String pattern = BeaglePreferencesPage.PATTERN_PREFERENCE_DEFAULT_VALUE;
    if (Activator.INSTANCE != null) {
      IPreferenceStore pStore = Activator.INSTANCE.getPreferenceStore();
      pattern = pStore.getString(BeaglePreferencesPage.PATTERN_PREFERENCE);
    }
    try {
      Parser<ILoggingEvent> p = new Parser<ILoggingEvent>(pattern);
      p.setContext(context);
      Node t = p.parse();
      head = p.compile(t, PatternLayout.defaultConverterMap);
      ch.qos.logback.beagle.util.ConverterUtil.setContextForConverters(context,
	  head);
      ConverterUtil.startConverters(head);
      fillConverterList();
    } catch (ScanException e) {
      Activator.INSTANCE.logException(e, e.getMessage());
    }
  }

  public String convert(ILoggingEvent event) {
    StringBuilder sb = new StringBuilder();
    Converter<ILoggingEvent> c = head;
    while (c != null) {
      sb.append(c.convert(event));
      c = c.getNext();
    }
    return sb.toString();
  }
  
  public List<Converter<ILoggingEvent>> getConverterList() {
    return converterList;
  }

  public int getColumnCount() {
    return converterList.size();
  }

  public void setConverterList(List<Converter<ILoggingEvent>> converterList) {
    this.converterList = converterList;
  }

  private void fillConverterList() {
    Converter<ILoggingEvent> c = head;
    while (c != null) {
      if (!(c instanceof LiteralConverter)) {
	converterList.add(c);
      }
      c = c.getNext();
    }
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public String computeConverterName(Converter<ILoggingEvent> c) {
    if (c instanceof CompositeConverter) {
      return computeCompositeConverterName((CompositeConverter<ILoggingEvent>) c);
    } else {
      return computeSimpleConverterName(c);
    }

  }

  private String computeCompositeConverterName(
      CompositeConverter<ILoggingEvent> compositeConverter) {
    StringBuilder nameSB = new StringBuilder("composite");
    Converter<ILoggingEvent> child = compositeConverter.getChildConverter();
    while (child != null) {
      if (!(child instanceof LiteralConverter))
	nameSB.append('_').append(computeSimpleConverterName(child));
      child = child.getNext();
    }
    return nameSB.toString();
  }

  private String computeSimpleConverterName(Converter<ILoggingEvent> c) {
    String className = c.getClass().getSimpleName();
    int index = className.indexOf("Converter");
    if (index == -1) {
      return className;
    } else {
      return className.substring(0, index);
    }
  }
}
