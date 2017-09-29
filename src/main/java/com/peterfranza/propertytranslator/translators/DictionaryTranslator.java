package com.peterfranza.propertytranslator.translators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peterfranza.propertytranslator.TranslationMasterDictionaryType;
import com.peterfranza.propertytranslator.TranslatorConfig;

public class DictionaryTranslator implements Translator {

	private Map<String, TranslationObject> dictionary = new HashMap<String, TranslationObject>();
	
	private TranslatorConfig config;

	private String sourceLanguage;

	public void reconfigure(TranslatorConfig config, String sourceLanguage) {
		this.config = config;
		this.sourceLanguage = sourceLanguage;
		dictionary.clear();
	}
	
	public String translate(String sourcePhrase) throws Exception {
		
		if(sourcePhrase == null)
			return null;
		
		if(sourcePhrase.isEmpty())
			return "";
		
		String key = calculateKey(sourcePhrase);
		TranslationObject target = dictionary.get(key);
		if(target != null)
			return target.targetPhrase;
		
		target = new TranslationObject();
		target.calculatedKey = key;
		target.sourcePhrase=sourcePhrase;
		dictionary.put(key, target);
		
		if(config.omitMissingKeys)
			return null;
		else 
			return "__UNKNOWN__";
	}

	public void open() throws IOException {
		dictionary.clear();
		dictionary.putAll(getDictionaryLoaderFor(config.dictionaryFormat).loadFile(config.dictionary));
	}

	public void close() throws IOException {
		getDictionaryLoaderFor(config.dictionaryFormat).saveFile(config.dictionary, dictionary);
		dictionary.clear();
	}
	
	private static String calculateKey(String sourcePhrase) throws Exception {
		sourcePhrase = sourcePhrase.toLowerCase().trim();
		MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		crypt.reset();
		crypt.update(sourcePhrase.getBytes());
		return byteToHex(crypt.digest());
	}

	private static String byteToHex(byte[] digest) {
		Formatter formatter = new Formatter();
		for(byte b: digest) {
			formatter.format("%02x", b);
		}
		String result = formatter.toString();
		formatter.close();
		return result;
	}

	public static class TranslationObject {	
		String calculatedKey;
		String sourcePhrase;
		String targetPhrase = "";
	}
	
	private DictionaryLoader getDictionaryLoaderFor(TranslationMasterDictionaryType type) {
		switch(type) {
		
			case PROPERTIES: return new PropertiesDictionaryLoader();
		
			default:
				return new JSONDictionaryLoader();
		}
	}
	
	private interface DictionaryLoader {

		Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException;
		void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException;
		
	}
	
	static class Dictionary {
		String sourceLanguage;
		String targetLanguage;
		Collection<TranslationObject> objects;
	}
	
	private class JSONDictionaryLoader implements DictionaryLoader {

		public Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException {
			if(masterDictionary.exists()) {
				HashMap<String, TranslationObject> md = new HashMap<>();
				try(Reader reader = new FileReader(masterDictionary)) {
					Dictionary d = new Gson().fromJson(reader, Dictionary.class);
					for(TranslationObject obj: d.objects) {
						md.put(obj.calculatedKey, obj);
					}
				}
				return md;
			}
			return new HashMap<>();
		}

		public void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException {
			masterDictionary.getParentFile().mkdirs();
			try(Writer writer = new FileWriter(masterDictionary)) {
				Dictionary d = new Dictionary();
				d.targetLanguage = config.targetLanguage;
				d.sourceLanguage = sourceLanguage;
				d.objects = dictionary.values();

				new GsonBuilder().setPrettyPrinting().create().toJson(d, writer);
			}
		}
		
	}
	
	private class PropertiesDictionaryLoader implements DictionaryLoader {

		@Override
		public Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException {
			if(masterDictionary.exists()) {
				HashMap<String, TranslationObject> md = new HashMap<>();
				try(Reader reader = new FileReader(masterDictionary)) {
					Properties p = new Properties();
					p.load(reader);
					
					for(Entry<Object, Object> es: p.entrySet()) {
						TranslationObject o = new TranslationObject();
						o.calculatedKey = es.getKey().toString();
						o.sourcePhrase = "";
						o.targetPhrase = es.getValue().toString();
						md.put(es.getKey().toString(), o);
					}
				}
				return md;
			}
			return new HashMap<>();
		}

		@Override
		public void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException {
			try(Writer writer = new FileWriter(masterDictionary)) {
				Properties p = new Properties();
				for(TranslationObject o: dictionary.values()) {
				
					p.setProperty(o.calculatedKey, cascade(o.targetPhrase, o.sourcePhrase, ""));
				}
				p.store(writer, "");
			}
		}

		private String cascade(String ...  string) {
			for(String s: string) {
				if(s != null && !s.isEmpty())
					return s;
			}
			return "";
		}
		
	}
	
	private class XLIFF12DictionaryLoader implements DictionaryLoader {

		@Override
		public Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException {
			// TODO Auto-generated method stub
			
		}
		
		
	}
	
}