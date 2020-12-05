package my_first_plugin.handlers;

/*
 * Classe importante per il calcolo del DIT, permette di conservare solo per le interfacce del progetto
 * il nome dell'interfaccia e la sua distanza dal nodo radice più lontano.
 */
public class DitInheritance {
	//Variabile di istanza per il nome dell'interfaccia
	private String name_interface;
	//Variabile di istanza per il livello di profondità dell'interfaccia nell'albero gerarchico
	private int level;
	
	public DitInheritance () {}
	
	/*
	 * Generazione automatica dei metodi get e set
	 */

	public String getName_interface() {
		return name_interface;
	}

	public void setName_interface(String name_interface) {
		this.name_interface = name_interface;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}