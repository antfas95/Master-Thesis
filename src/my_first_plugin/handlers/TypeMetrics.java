package my_first_plugin.handlers;

/*
 * Con questo mio oggetto assemblo quelle che sono le informazioni riscontrate sia per dare un giudizio quantificativo
 * sia sull'ereditarietà di specifica, sia per l'ereditarietà di implementazione, per ogni singolo tipo riscontrato in
 * ogni singolo progetto. Inoltre sarà importante questa definizione per andare poi a prendere informzaioni dal csv costruito
 * da Metrics ed integrarlo con le informazioni da me catturate per definire il livello di ereditarietà di specifica e di
 * implementazione per ogni singolo tipo del progetto.
 */
public class TypeMetrics {
	/*
	 * Definiscono le variabili di istanza che riguardano tutte le metriche collegabili ad un tipo definito
	 * Rappresentano tutte quelle metriche calcolate e definite all'interno del mio plugin
	 */
	//Nome del progetto del tipo in esame
	private String project_name;
	//Nome del tipo in esame
	private String nome_tipo;
	//Valore associato al tipo sull'ereditarietà di specifica
	private int inh_spec;
	//Valore numerico associato al tipo sull'ereditarietà di implmentazione
	private int delegation;
	//Valore numerico associato alla profondità della classe in questione
	private int dit;
	//Valore di accoppiamento della classe
	private int cbo;
	//Valore per la metrica response for a class di C&K
	private int rfc;
	//Variabile di istanza per quantificare il valore di ereditarietà di implementazione
	private int inh_impl;
	//Variabile per una seconda definizione di ereditarietà di implementazione
	private int delegation_two;
	//Variabile per conteggiare il numero di LOC vincolanti all'inteno di un determinato tipo
	private int LOC;
	//Variabile di istanza ceh conta semplicemente il numero di metodi presenti sia nella sottoclasse sia in una delle sue superclassi
	private int n_methods;

	/*
	 * Definisco le metriche di tipo che invece sono ritornate dal plugin_metrics
	 */
	private int dit_metrics;
	private int wmc;
	//Valore associato al numero dei children
	private int noc;
	//Variabile di istanza associata al valore dei metodi sovrascritti inerenti alla classe in esame
	private int norm;
	//Lack of cohesion
	private double lcom;
	//Numero degli attributi
	private int na;
	//Numero di attributi statici presenti nella classe
	private int nsf;
	//Numero dei metodi
	private int nom;
	//Numero dei metodi statici
	private int nsm;
	//Valore di specializzazione della classe
	private double six;

	/*
	 * Definisco costruttore vuoto per poter fare anche il set delle varie variabili di istanza
	 */
	public TypeMetrics() {}

	public int getLOC() {
		return LOC;
	}

	public void setLOC(int lOC) {
		LOC = lOC;
	}
	
	public int getDelegation() {
		return delegation;
	}

	public void setDelegation(int delegation) {
		this.delegation = delegation;
	}

	public int getDelegation_two() {
		return delegation_two;
	}

	public void setDelegation_two(int delegation_two) {
		this.delegation_two = delegation_two;
	}

	public int getCbo() {
		return cbo;
	}

	public void setCbo(int cbo) {
		this.cbo = cbo;
	}
	
	public double getSix() {
		return six;
	}

	public void setSix(double six) {
		this.six = six;
	}
	
	public int getN_methods() {
		return n_methods;
	}

	public void setN_methods(int n_methods) {
		this.n_methods = n_methods;
	}

	public int getNsm() {
		return nsm;
	}

	public void setNsm(int nsm) {
		this.nsm = nsm;
	}

	public int getNsf() {
		return nsf;
	}

	public void setNsf(int nsf) {
		this.nsf = nsf;
	}

	public int getNorm() {
		return norm;
	}

	public void setNorm(int norm) {
		this.norm = norm;
	}

	public int getDit_metrics() {
		return dit_metrics;
	}

	public void setDit_metrics(int dit_metrics) {
		this.dit_metrics = dit_metrics;
	}

	public String getProject_name() {
		return project_name;
	}

	public void setProject_name(String project_name) {
		this.project_name = project_name;
	}

	public String getNome_tipo() {
		return nome_tipo;
	}

	public void setNome_tipo(String nome_tipo) {
		this.nome_tipo = nome_tipo;
	}

	public int getInh_spec() {
		return inh_spec;
	}

	public void setInh_spec(int inh_spec) {
		this.inh_spec = inh_spec;
	}

	public int getInh_impl() {
		return inh_impl;
	}

	public int getNoc() {
		return noc;
	}

	public void setNoc(int numom) {
		this.noc = numom;
	}

	public void setInh_impl(int inh_impl) {
		this.inh_impl = inh_impl;
	}

	public int getRfc() {
		return rfc;
	}

	public void setRfc(int rfc) {
		this.rfc = rfc;
	}

	public int getDit() {
		return dit;
	}

	public void setDit(int dit) {
		this.dit = dit;
	}

	public int getWmc() {
		return wmc;
	}

	public void setWmc(int wmc) {
		this.wmc = wmc;
	}

	public double getLcom() {
		return lcom;
	}

	public void setLcom(double lc) {
		this.lcom = lc;
	}

	public int getNa() {
		return na;
	}

	public void setNa(int na) {
		this.na = na;
	}

	public int getNom() {
		return nom;
	}

	public void setNom(int nom) {
		this.nom = nom;
	}
}