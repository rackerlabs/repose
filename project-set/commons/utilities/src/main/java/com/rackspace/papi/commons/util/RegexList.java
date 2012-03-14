package com.rackspace.papi.commons.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexList {

   private final List<Pattern> regexMatchers;

   public RegexList() {
      this.regexMatchers = new LinkedList<Pattern>();
   }

   public void add(String newRegexTarget) {
      regexMatchers.add(Pattern.compile(newRegexTarget));
   }

   public Matcher find(String target) {
      for (Pattern targetPattern : regexMatchers) {
         final Matcher matcherRef = targetPattern.matcher(target);

         if (matcherRef.find()) {
            return matcherRef;
         }
      }

      return null;
   }

   public Matcher matches(String target) {
      for (Pattern targetPattern : regexMatchers) {
         final Matcher matcherRef = targetPattern.matcher(target);

         if (matcherRef.matches()) {
            return matcherRef;
         }
      }

      return null;
   }
}
