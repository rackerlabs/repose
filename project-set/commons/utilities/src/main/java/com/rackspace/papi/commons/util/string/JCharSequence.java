package com.rackspace.papi.commons.util.string;

/**
 * Given the fact that StringBuilder, StringBuffer and String only share CharSequence
 * as a common interface I decided to enhance that particular idiom and create a
 * few wrapper classes to accommodate it.
 * 
 * What this allows a programmer to do is wrap any one of the above objects using
 * this interface and interact with it more richly than the CharSequence interface
 * allows all while avoiding creating needless instances of String.
 * 
 * @author zinic
 */

// TODO:Rename - This class needs to be renamed to something a little better, maybe...
public interface JCharSequence extends CharSequence {

    CharSequence asCharSequence();

    int indexOf(String seq);

    int indexOf(String seq, int fromIndex);
}
