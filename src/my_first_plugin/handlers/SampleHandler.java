package my_first_plugin.handlers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.util.IMethodInfo;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

public class SampleHandler extends AbstractHandler implements IConsoleListener, IPatternMatchListener, IWorkbenchWindowActionDelegate {

	/**
	 * The constructor.
	 */

	private String pattern;
	private int matchCount = 0;
	private IWorkbenchWindow window;

	//Variabili di istanza importanti per estrapolare le informazioni per JDT Core
	private IWorkspace workspace;
	private IWorkspaceRoot root;
	private IJavaProject project;
	private String nome_progetto;

	//Variabile di istanza che fanno riferimento alle metriche di Chidamber and Kremerer
	//Variabile di istanza per definire il livello di accoppiamento della classe in esame
	int cbo = 0;
	//Variabile di istanza utile per definire il numero di figli direttamente vincolanti al tipo in esame
	//int noc = 0;
	//Variabile di istanza per il calcolo di LCOM
	//double LCOM = 0;
	//Variabile di istanza per il calcolo della metrica RFC
	int rfc = 0;
	/*
	 * Variabili di istanza utili per calcolare altre metriche inerenti al tipo di un progetto
	 */
	//Variabile per ottenere tutti i metodi di una classe
	//private int numberOfMethods = 0;
	//Variabile per ottenere tutte le variabili di istanza di una classe
	private int numberOfFields = 0;
	//ereditarietà di implementazione con unico conteggio della variabile di istanza utilizzata, conta una sola occorrenza
	private int delegation_two = 0;
	// Conta il valore di delegazione ma considerando tutte le occorrenze riferite a una determinata variabile di istanza
	private int delegation = 0;
	//Vsalore associato al calcolo dell'ereditarietà di implementazione
	private int ereditarietà_implementazione = 0;
	//Variabile di istanza che mantiene solo il numero di metodi dello stesso nome presenti sia nella superclasse che nella sottoclasse
	private int n_metodi_uguali = 0;
	//Numero dei LOC presenti all'interno di una classe
	private int LOC = 0;
	//Valore associato per definire la soglia di delegazione

	//Variabili di istanza utili per il calcolo del DIT inerente alle interfacce
	ArrayList<DitInheritance> inhInter = new ArrayList<DitInheritance>();
	//Variabile di istanza importante per ottenere per ogni tipo tutte le classi estese ed il livello della scoperta della classe
	ArrayList<DitInheritance> inhFType = new ArrayList<DitInheritance>();
	private int max_dit = 0;
	private int level = 0;

	//Variabili di istanza che mi servono per il metodo che calcola l'ereditarietà di specifica
	ArrayList<String> all_interface = new ArrayList<String>();
	ArrayList<String> all_extends = new ArrayList<String>();
	//Definisco arrayList che conserva tutti i riferimenti del tipo
	ArrayList<TypeMetrics> all_metrics = new ArrayList<TypeMetrics>();
	String appoggio;

	public SampleHandler() {
		/*
		 * Metodo costruttore quando viene eseguito il plugin viene lanciato direttamente il metodo
		 * matchFound che eleabora le informazioni necessarie per il calcolo delle dovute metriche
		 */
		this.matchFound(null);
		System.out.println("Eseguo Sample Handler");
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Eseguo execute");
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
				window.getShell(),
				"Dati elaborati correttamente...",
				"Controlla il csv generato al path definito come output");
		return null;
	}

	@Override
	public void consolesAdded(IConsole[] consoles) {
		// TODO Auto-generated method stub
		for(IConsole console : consoles) {
			if(console instanceof TextConsole) {
				((TextConsole) console).addPatternMatchListener(this);
			}
		}

	}

	@Override
	public void consolesRemoved(IConsole[] consoles) {
		// TODO Auto-generated method stub
		for(IConsole console : consoles) {
			if(console instanceof TextConsole) {
				((TextConsole) console).removePatternMatchListener(this);
			}
		}
	}

	@Override
	public void connect(TextConsole arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void matchFound(PatternMatchEvent arg0) {
		// TODO Auto-generated method stub
		//In input definisco il tipo di progetto da analizzare
		this.nome_progetto = JOptionPane.showInputDialog("Digita il nome del progetto che vuoi esaminare: ");
		//Ottengo tutti i progetti
		IProject[] projects = ottieniProgetti();

		//Variabile che mantiene la lista dei nomi di tutti i tipi del progetto
		ArrayList<String> alltypes = new ArrayList<String>();
		try {
			//Ottengo tutti i packages di un determinato progetto
			IPackageFragment[] packages = ottieniPackages(projects);
			//Ottengo tutti i CompilationUnits di ogni packages del progetto
			ArrayList<ICompilationUnit> comp_units = ottieniCompUnits(packages);
			//Richiamo il metodo che estrapola tutti i tipi partendo dai Compilation Unit riscontrati
			ArrayList<IType> types = ottieniTipi(comp_units);
			//Ottenuti i tipi verifico quali tipi risultano essere delle interfacce
			ArrayList<IType> interfaces = new ArrayList<IType>();
			interfaces = estrapolaInterfacce(types);
			/*
			 * A questo punto su ogni sigolo tipo andiamo a chiamare il metodo che permette di calcolare l'ereditarietà
			 * di specifica e di implementazione, ovviamente per ogni tipo esaminato le varie informazioni devono essere
			 * memorizzate in un oggetto apposito, utile per conservare tutte le informazioni calcolate e definite.
			 */
			//Definisco arrayList che conserva tutti i riferimenti del tipo
			all_metrics = new ArrayList<TypeMetrics>();

			//Richiamo il metodo capace di ritornare per ogni interfaccia del progetto il suo livello di profondità
			//calcolaInhForInterfaces (interfaces, types);

			//Per ogni tipo a questo punto andiamo a calcolare tutti i tipi di metriche di riferimento
			for (IType type: types) {
				/*
				 * Visto che risulterà essere utile per calcolare sia ereditarietà di specifica che di implementazione,
				 * per evitare duplicazione di codice andiamo ad estrapolare quelli che sono i metodi vincolanti al tipo
				 * in questione
				 */
				// Inizializzo le variabile che devono essere vincolanti a questo punto solo per il tipo in esame e in questione
				this.delegation = 0;
				this.delegation_two = 0;
				this.ereditarietà_implementazione = 0;
				this.n_metodi_uguali = 0;
				//System.out.println("Tipo esaminato: " + type.getElementName());
				IMethod[] methods = ritornaMetodi(type);
				//Osservo il numero di metodi vincolanti all'interno della classe in esame
				//this.numberOfMethods = methods.length; //Warning attributo ridondante informazioni già ottenute precedentemente
				alltypes.add(type.getElementName());
				//Osservo tutti i figli della classe type in esame
				//calcolaNoc(type, types);
				//Osservo la mancanza di coesione nei metodi della classe in esame
				//System.out.println("Per la classe: " + type.getElementName() + "Il valore del noc è: " + this.noc);
				//Definisco due interi che rappresenteranno lo score relativo a ereditarietà di specifica e di implementazione
				int inh_impl = 0;
				int inh_spec = 0;
				//Metodo che permette di calcolare il valore di ereditarietà di implementazione per la classe type
				inh_impl = ereditarietà_Implementazione(type, types, methods);
				//Stampa di tutti i tipi, con associato il valore dell'ereditarietà di implementazione
				//System.out.println("Per la classe: " + type.getElementName() + " il valore di ereditarietà di implementazione è pari a : " + inh_impl);
				//Metodo che permette di calcolare il valore di ereditarietà di specifica per la classe type 
				inh_spec = ereditarietà_specifica (type, types, methods);
				System.out.println("Per il tipo: " + type.getElementName() + ", l'ereditarietà di implementazione è: " + inh_impl + ", delegazione: " + this.delegation + ", specifica: " + inh_spec);
				//System.out.println("Per il tipo: " + type.getElementName() + ", l'ereditarietà di implementazione è: " + this.ereditarietà_implementazione + ", con metodi uguali pari a: " + this.n_metodi_uguali + ", delegazione: " + this.delegation);
				//System.out.println("Per la classe: " + type.getElementName() + " il valore di ereditarietà di specifica è pari a : " + inh_spec);
				//inh_spec = calcola_Inh_Spec (type, types, methods);

				//A questo punto per il tipo in esame del for vado a calcolare ricorsivamente tutti i sottotipi
				//calcola_dipendenzeDIT(type, types);
				//Metodo che permette di calcolare in maniera precisa la metrica DIT
				//int metrica_dit = calcola_DIT(type, types);
				//System.out.println("Dit ottenuto: " + metrica_dit + "per il tipo: " + type.getElementName());

				/*
				 * A questo punto effettuo istanzio un oggetto di tipo TypeMetrics per assegnare i giusti valori inerenti
				 * all'ereditarietà di implementazione e di specifica
				 */
				TypeMetrics metrica = new TypeMetrics();
				//Effettuo il set del nome del progetto:
				metrica.setProject_name(nome_progetto);
				//Effettuo il set del nome del tipo in esame
				metrica.setNome_tipo(type.getElementName());
				//Effettuo il set del valore dell'ereditarietà di implementazione
				metrica.setInh_impl(inh_impl);
				//Effettuo il set del valore dell'ereditarietà di specifica
				metrica.setInh_spec(inh_spec);
				//Effettuo il set della metrica cbo per il type appena scansionato
				metrica.setCbo(this.cbo);
				//Effettuo il set della metrica dit per il type appena scansionato, Non ritornato nel file perchè preso quello di Metrics
				metrica.setDit(this.max_dit);
				//Effettuo il set della metrica rfc per il type appena scansionato
				metrica.setRfc(this.rfc);
				//Effetto il set della metrica noc per il type appena scansionato
				//metrica.setNoc(this.noc);
				//Effettuo il set della metrica lcom per il type appena scansionato
				//metrica.setLcom(this.LCOM);
				//Effettuo il set della metrica per il numero dei metodi nella classe
				//metrica.setNom(this.numberOfMethods);
				//Effettuo il set della metrica per il numero dei campi vincolanti nella classe in esame
				//metrica.setNa(this.numberOfFields);
				//Effettuo il set della metrica che considera il livello di delegazione considerando tutte le occorrenze
				metrica.setDelegation(this.delegation);
				//Effettuo il set della metrica per il secondo valore inerente alla delegazione
				metrica.setDelegation_two(this.delegation_two);
				//Effettuo il set del valore inerente all'ereditarietà di implementazione
				//metrica.setInh_impl(this.ereditarietà_implementazione);
				//Effettuo il set del numero di metodi ritornati
				metrica.setN_methods(this.n_metodi_uguali);
				//Effettuo il set della metrica per il calcolo del numero dei LOC presenti all'interno di una classe + 2 per il nome della classe e la } di chiusura della classe
				//Disabilito in quanto prelevato quello ritornato da Metrics
				metrica.setLOC(this.LOC + this.numberOfFields + 2);
				//Aggiungo il riferimento all'array che mantiene le metriche per ogni singolo tipo
				all_metrics.add(metrica);
			}
			//Stampo il contenuto dell'array di tutte le metriche ritrovate
			int i = 0;
			for (TypeMetrics t : all_metrics) {
				i++;
				//System.out.println("Nome tipo: " + t.getNome_tipo() + ", dit: " + t.getDit() + ", inh: " + t.getInh_impl() + ", spec: " + t.getInh_spec() + ", wmc: " + t.getWmc() + ", nsc: " + t.getNumom() + ", noms: " + t.getNoms() + ", lc: " + t.getLc() + ", nof: " + t.getNa() + ", nsf: " + t.getNsa() + ", nom: " + t.getNom() + ", nsm: " + t.getNms() + ", is: " + t.getIs() + ", cbo: " + t.getCbo());
			}
			//System.out.println("Numero di occorrenze totali: " + i);

			/*
			 * Dopo aver letto e aggregato correttamente il contenuto delle metriche legate al tool Metrics a questo punto
			 * dobbiamo essere in grado di andare a creare un nuovo file csv comprensivo di tutte le metriche vincolanti
			 * per ogni singolo tipo in question
			 */
			read_csv_Metrics(alltypes);

			//Metodo che permette di effettuare la scrittura del file definitivo
			write_CSV(all_metrics);

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Metodo che permette di estrapolare tutti i progetti di una determinata workspace	
	 */

	public IProject[] ottieniProgetti() {
		//Passi necessario per poter prendere riferimento alla workspace che contiene i progetti da esaminare
		this.workspace = ResourcesPlugin.getWorkspace();
		this.root = workspace.getRoot();
		//Ottengo tutti i progetti appena istanziati
		IProject[] projects = root.getProjects();
		//Semplice stampa per osservare i risultati ottenuti
		//System.out.println("Mi trovo nel metodo nel quale ottengo i progetti!!");
		//System.out.println("Ecco i progetti ottenuti: ");
		/*
		 * Stampa di questo attivata solo quando serve per osservare i risultati ottenuti
		for (IProject p : projects) {
			System.out.println("Nome del progetto: " + p.getName());
		}
		 */
		return projects;
	}

	/*
	 * Metodo che permette di estrapolare tutti i packages di un determinato progetto
	 * @param projects -> passato dal metodo originale.	
	 */
	public IPackageFragment[] ottieniPackages(IProject[] projects) throws CoreException {
		//Prendo il riferimento al progetto "tester" oggetto dello studio
		IProject progetto_tester = root.getProject(this.nome_progetto);
		//Istanzio quello che sarà il ritorno del metodo
		IPackageFragment[] packages = null;
		//Ovviamente dobbiamo verificare che il progetto in esame sia un progetto Java
		if (progetto_tester.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
			//Creo l'istanza del mio progetto tester
			this.project = JavaCore.create(progetto_tester);
			//Dal progetto creato a questo punto ottengo tutti i Packages a lui vincolanti
			packages = this.project.getPackageFragments();
			//Per la verifica stampo la lista dei packages ottenuti, stampa attivata solo quando serve
			/*
			for (IPackageFragment pack : packages) {
				System.out.println("Ecco il nome del packages: " + pack.getElementName());
			}
			 */
		}
		return packages;
	}

	/*
	 * Metodo che permette di estrapolare tutti i CompilationUnit (file con estensione .java) di un determinato packages
	 * di un determinato progetto
	 * @param packages -> passato dal metodo originale.	
	 */

	public ArrayList<ICompilationUnit> ottieniCompUnits(IPackageFragment[] packages) throws JavaModelException {
		//Array di ritorno
		ICompilationUnit[] comp_unit = null;
		ArrayList<ICompilationUnit> compis = new ArrayList<ICompilationUnit>();
		//Variabile per il conteggio dei comp Unit ritornati
		int comps = 0;
		//For per iterare ogni package del progetto
		for (IPackageFragment pack : packages) {
			//Verifico l'esistenza di codice sorgente nel packages
			if (pack.getKind() == IPackageFragmentRoot.K_SOURCE) {
				/*
				 * Aggiungo le classi vincolanti del package a quelle del progetto e aggiungo ognuno di loro nel mio 
				 * arrayList. Effettuo questo trasferimento per poter utilizzare in maniera concisa le funzionalità che 
				 * l'arraylist mi mette a disposizione, mentre l'array no
				 */
				comp_unit = pack.getCompilationUnits();
				for (ICompilationUnit c : comp_unit) {
					compis.add(c);
					comps++;
				}
			}
		}
		//Stampo per verifica l'elenco di tutti i CompilationUnit ottenuti ed estrapolati. Attivare solo quando serve
		/*
		for (ICompilationUnit comp : comp_unit) {
			System.out.println("Nome del compUnit: " + comp.getElementName());
		}
		//Piccola stampa per poter verificare il numero di compilationUnit ritornati
		System.out.println("Complessivamente vengono ritornate: " + comps + " CompilationUnit");
		 */
		return compis;
	}

	/*
	 * Metodo che permette di estrapolare tutti i tipi partendo dai Compilation Unit ottenuti
	 * di un determinato progetto
	 * @param comps -> passato dal metodo originale.	
	 */
	public ArrayList<IType> ottieniTipi(ArrayList<ICompilationUnit> comps) throws JavaModelException{
		ArrayList<IType> allTypes = new ArrayList<IType>();
		//Variabile per contare tutti i tipi ritornati
		int ty = 0;
		//Ciclo tutti i Compilation Unit e per ognuno estrapolo i vari tipi
		for (ICompilationUnit c : comps) {
			//Metodo che mi permette di estrapolare tutti i tipi di una classe
			IType[] tipi = c.getAllTypes();
			//Con il for aggiungo tutti i tipi all'ArrayList
			for (IType t : tipi) {
				allTypes.add(t);
			}
		}
		//Stampo i risultati ottenuti. Da attivare solo quando effettuare test per visionare l'output
		/*
		for (IType t : allTypes) {
			System.out.println("Nome del tipo: " + t.getElementName());
			ty++;
		}
		System.out.println("In totale ho ritrovato: " + ty);
		 */
		return allTypes;
	}

	/*
	 * Metodo che ritorna tutte le interfacce partendo dai tipi riscontrati e vincolanti in un progetto
	 * @param types -> arrayList di tutti i tipi vincolanti nel programma
	 * @return interfaces -> arrayList di tutte le interfacce ritrovate dai tipi passati come paramentro
	 */
	public ArrayList<IType> estrapolaInterfacce (ArrayList<IType> types) throws JavaModelException{
		//Istanzio arrayList di ritorno
		ArrayList<IType> interfaces = new ArrayList<IType>();
		//Con un for controllo la presenza dell'interfaccia
		for (IType type: types) {
			//Con il metodo isInterface() di JDT verifico se è un'interfaccia
			if (type.isInterface()) {
				interfaces.add(type);
			}
		}
		//Al termine del for stampo quelle che sono le interfacce riscontrate nel programma
		/*
		 * Da attivare solo per il test e verificare che il metodo ritorna le dovute informazioni
		int i = 0;
		for (IType type : interfaces) {
			i++;
			System.out.println("Ecco l'interfaccia numero: " + i + " con il seguente nome: " + type.getElementName());
		}
		 */
		return interfaces;
	}

	/*
	 * Metodo che permette di ritornare tutti i metodi di un detrminato tipo passato come paramentro
	 * @param type -> tipo, classe sul quale verranno ritornati tutti i metodi vincolanti
	 */
	public IMethod[] ritornaMetodi (IType type) throws JavaModelException {
		//Utilizzo il metodo di JDT getMethods per ritornare tutti i metodi di interesse
		IMethod[] methods = type.getMethods();
		//System.out.println("Nome del tipo: " + type.getElementName());
		/*
		 * Solo per verifica stampo il nome di tutti i metodi in modo tale da poter verificare che vengano ritornati
		 * tutti i metodi di interesse
		 */
		/*
		int num = 0;
		for (IMethod method : methods) {
			num++;
			System.out.println("Nome metodo: " + method.getElementName().toString());
		}
		System.out.println("Numero dei metodi in totale: " + num);
		 */
		return methods;
	}

	/*
	 *
	 *
	 *									METODI PRINCIPALI INERENTI AL CALCOLO DELL'EREDITARIETA'
	 *										DI IMPLEMENTAZIONE ED EREDITARIETA' DI SPECIFICA
	 *
	 *
	 */

	/*
	 * Metodo che verifica quante volte la classe richiama metodi che sono definiti all'interno della classe stessa
	 */
	public int verificaMetodoClass (String sorgente_Metodo, IMethod[] methods) {
		int ritorno = 0;
		for (IMethod metodo : methods) {
			if (sorgente_Metodo.contains("." + metodo.getElementName())) {
				//Aumento il valore di ritorno
				ritorno++;
			}
		}
		return ritorno;
	}
	/*
	 * Metodo inerente al calcolo dell'ereditarietà di implementazione
	 */
	public int ereditarietà_Implementazione (IType type, ArrayList<IType> types, IMethod[] methods) throws JavaModelException {
		//Array che mantiene tutti i tipi del progetto
		ArrayList<String> types_proj = new ArrayList<String>();
		//Array di tutti i campi della tipo in esame
		IField[] fields = type.getFields();

		//Definisco il set del numero di classi vincolanti per il tipo in esame
		this.numberOfFields = fields.length;

		//Quando prendo il riferimento ai campi posso richiamare il metodo per il calcolo di LCOM
		//calcolaLCOM (type, fields, methods);
		//Metodo che permette di calcolare il valore associato al valore di LCOM
		this.rfc = 0;
		for (IMethod metodo : methods) {
			String pulito = cleanMethod(metodo);
			//Per il metodo verifico quanti metodi della classe vengono richiamati
			int verifica = verificaMetodoClass (pulito, methods);
			//For utile per quantificare il valore di rfc
			for (int i = 0; i < pulito.length(); i++) {
				if(pulito.charAt(i) == '.') {
					this.rfc++;
				}
			}
			//Al termino del for sottraggo il valore di rfc riscontrato con il valore ottenuto osservando il numero di metodi della classe richiamati
			this.rfc = this.rfc - verifica;
		}

		//Inserisco in un ArrayList tutti i tipi del progetto //FOR inutile perchè in un parametro già si possiede il riferimento a tutti i tipi
		for (IType tipo : types) {
			types_proj.add(tipo.getElementName());
		}

		//Istanzio ArrayList per tutti i tipi delle variabili di istanza della classe
		ArrayList<String> all_types = new ArrayList<String>();

		this.cbo = 0;
		for (IField field : fields) {
			//Ottengo il riferimento al tipo del campo
			String type_field = field.getTypeSignature().toString();
			//Controllo se è un tipo basilare di Eclipse
			if (!(type_field.length() == 1)) {
				//Se non è uguale ad uno la lunghezza prendo il riferimento del tipo del campo (variabile d'istanza)
				type_field = type_field.substring(1, type_field.length()-1);
				//Controllo per verificare se non si tratta di un altro tipo basilare String
				if (!(type_field.equals("String"))){
					all_types.add(type_field);
					//Boolean che verifica se il tipo della variabile di istanza è conforme ad un tipo del progetto
					boolean check_control = false;
					//Istruzione utile per verificare la presenza di un eventuale match (tipo istanza - tipo del progetto)
					check_control = types_proj.contains(type_field);
					if (check_control == true) {
						//Incremento per il tipo in esame il valore inerente alla metrica cbo
						this.cbo++;
						//Match rilevato verificare se la variabile con match è utilizzata nei metodi del tipo in esame
						//Definisco variabile per contare una sola occerrenza e non tutte 
						boolean controllo = false;
						for (IMethod method: methods) {
							//Pulizia del metodo da eventuali commenti
							String variabile = cleanMethod(method);
							//Verifico se nel metodo esiste il nome della variabile di istanza seguita da un .
							boolean verify = variabile.contains(field.getElementName() + ".");
							if (verify == true) {
								//In caso di esito positivo incremento il contatore che afferma il livello di ereditarietà di implementazione
								this.delegation++;
								controllo = true;
							}
						}
						if (controllo == true) {
							this.delegation_two++;
						}
					}
				}
			}
		}

		/*
		 * Altro fatto rilevante che può essere utilizzato per il valore inerente all'ereditarietà di implementazione
		 * è la possibilità di osservare l'uso di metodi della superclasse nei metodi della classe in esame
		 */
		//Dichiaro ArrayList capace di mantenere riferimento a tutti i punti di estensione della classe che sto esaminando
		ArrayList<IType> all_extends = new ArrayList<IType>();
		String superclasse = type.getSuperclassName();
		if (superclasse != null) {
			while (superclasse != null) {
				boolean check = false;
				//Estrapolo il tipo
				for (IType t : types) {
					if (t.getElementName().equals(superclasse)) {
						check = true;
						superclasse = t.getSuperclassName();
						all_extends.add(t);
					}
				}

				if (check == false) {
					superclasse = null;
				}
			}
		}

		/*
		 * A questo punto esamino tutti i metodi della classe type con i metodi di tutte le estensioni
		 */
		//ArrayList di tutti i tipi da controllare nella classe type
		ArrayList<IMethod> all_extend_methods = new ArrayList<IMethod>();
		for (IType tipo: all_extends) {
			IMethod[] metodi_supeclasse = tipo.getMethods();
			for (IMethod m : metodi_supeclasse) {
				boolean checkesistenza = false;
				for (IMethod metodino : all_extend_methods) {
					if (metodino.getElementName().equals(m.getElementName())) {
						checkesistenza = true;
					}
				}
				if (checkesistenza == false) {
					all_extend_methods.add(m);
				} 
			}
		}

		/*
		 * Dichiarazione di una nuova variabile che come l'ereditarietà di implementazione verifica se e solo se un metodo è stato
		 * richiamato all'interno della classe senza considerare il numero di occorrenze
		 */
		int ere_impl_2 = 0;
		for (IMethod metodo: all_extend_methods) {
			boolean use_methods = false;
			for (IMethod metodo_classe : methods) {
				String metodoClass = cleanMethodnoSignature(metodo_classe);
				//COntrollo se viene richiamato il metodo della superclasse con parola chiave super
				if (metodoClass.contains(metodo.getElementName() + "(")) {
					//System.out.println(metodoClass);
					use_methods = true;
					//System.out.println("Match tra metodo: " + metodo.getElementName() + ", e: " + metodoClass);
					this.ereditarietà_implementazione++;
				}
			}
			if (use_methods == true) {
				ere_impl_2++;
			}
		}
		//Prendo il tipo della superclasse
		return ere_impl_2;
	}

	/*
	 * Metodo che verifica se i metodi della classe estesa t vengono richiamati all'interno dei metodi della classe
	 * estendente
	 */
	public String verifica_ChiamataMetodi (IType superclasse, IType sottoclasse) throws JavaModelException {
		//Prendo tutti i metodi della superclasse
		IMethod[] metodi_superclasse = superclasse.getMethods();
		//Prendo tutti i metodi della sottoclasse
		IMethod[] metodi_sottoclasse = sottoclasse.getMethods();
		//Effettuo il for per tutti i metodi della sottoclasse
		//Valore boolean che ne verifica solo l'esistenza, eventualmente variabile di istanza
		boolean verifica_esistenza = false;
		/*
		 * For che permette di descrivere il vero valore di ereditarietà di implementazione, dove verifichiamo un'eventuale presenza di metodi
		 * della superclasse richiamati all'interno dei metodi della sottoclasse
		 */
		for (IMethod metodo : metodi_sottoclasse) {
			//Effettuo la pulizia del metodo
			String metodo_pulito = cleanMethod(metodo);
			//Valore numerico che conta quanti metodi della superclasse sono richiamati
			for (IMethod metodo_superclasse : metodi_superclasse) {
				if (metodo_pulito.contains(metodo_superclasse.getElementName())) {
					System.out.println("Metodo richiamo all'interno della classe");
					verifica_esistenza = true;
					this.ereditarietà_implementazione++;
				}
			}
		}
		return superclasse.getSuperclassName();
	}

	/*
	 * Metodo definitivo per il calcolo dell'ereditarietà di specifica
	 */

	public int ereditarietà_specifica (IType type, ArrayList<IType> types, IMethod[] methods) throws JavaModelException {
		//Istanzio la variabile di ritorno
		int val_inh_spec = 0;
		//Variabile appoggio che mantiene in maniera precisa l'ultima Stringa inerente all'ultimo punto di estensione
		this.appoggio = null;
		//Istanzio ArrayList per memorizzare tutte le interfacce riscontrate
		this.all_interface = new ArrayList<String>();
		//Istanzio ArrayList per memorizzare tutte le estensioni riscontrate
		this.all_extends = new ArrayList<String>();
		//ArrayList di tipi ricercati utile per non ripetere sempre gli stessi calcoli
		ArrayList<IType> types_discover = new ArrayList<IType>();

		String classe_estesa = type.getSuperclassName();

		//Verifico se ci sono classe estese
		if (!(classe_estesa == null)) {
			//Se esiste parola chiave extends aggiungo all'array di tutte le estensioni
			all_extends.add(classe_estesa);
			//Stringa di appoggio per gestire la chiamata ricorsiva
			//Riferimento alla superclasse con parola chiave implements
			String[] interface_implementate = type.getSuperInterfaceNames();
			if (!(interface_implementate == null)) {
				for (String c : interface_implementate) {
					this.all_interface.add(c);
					//System.out.println("Implemento la mia interfaccia: " + c);
				}
			}

			this.appoggio = classe_estesa;

			//Variabile che mi permette di gestire il ciclo while successivo
			boolean check = true;

			//finchè check non è false
			while (check) {
				/*
				 * In appoggio (riferimeno alla classe con parola chiave extends del tipo che in quel momento 
				 * risulta essere presente nella stringa appoggio).
				 * Il metodo new_Classes non fa altro che ritrovare IType di riferimento per la variabile appoggio
				 * e a quel punto estrapolare un eventuale tipo sotto parola chiave extends
				 */
				appoggio = new_Classes(types);
				//Controllo se il risultato risulta essere di uscita
				if (appoggio == "exit") {
					//Esco dal while
					check = false;
				} else {
					//Ho riscontrato una nuova estensione sotto parola chiave extends
					//Prima di aggiungere la classe verifico se essa già è presente nell'array che contiene tutte le estensioni
					if (!(all_extends.contains(appoggio))) {
						//System.out.println("Aggiungo la classe: ");
						all_extends.add(appoggio);
					}
				} 
			}

			/*
			 * Verifico corrispondenza di ogni tipo con ogni stringa prensente in all extends
			 * Inoltre con questo for permetto di aggiungere tutte le interfaccie vincolanti alle classi
			 * estese con punto chiave extends di una determinata classe scoperta.
			 */
			for (IType t : types) {
				for (String s : all_extends) {
					if (t.getElementName().contentEquals(s)) {
						//Aggiungo la classe ricercata come tipo all'array inzialmente istanziato
						types_discover.add(t);
						// Se si trova corrispondeza strampo tutte le interfacce del tipo in esame, sotto implements
						String[] interfacce_nuove = t.getSuperInterfaceNames();
						//Aggiungo a all_interface solo le nuove interfacce, non quelle già scoperte
						for (String nuova: interfacce_nuove) {
							if (!(all_interface.contains(nuova))) {
								all_interface.add(nuova);
							}
						}
					}
				}
			}

			/*
			 * Incremento il valore associato all'ereditarietà di specifica per la classe in esame, sommando
			 * al valore di ritorno il numero di tutte le interfaccie ritornate.
			 */
			val_inh_spec = val_inh_spec + all_interface.size();
			//System.out.println("Valore delle interfacce: " + all_interface.size());

			//For per ciclare tutti i tipi delle classi estese
			for (IType t : types_discover) {
				//Estrapolo tutti i metodi del tipo scoperto
				IMethod[] methods_discover = t.getMethods();
				//Variabile che mi fa capire se per il tipo in esame esiste una discrepanza
				boolean check_methods = true;
				/*
				 * Doppio for dove:
				 * - Il primo permette di ciclare i metodi della classe estendente
				 * - Il secondo permette di ciclare i metodi e rappresenta un parametro del metodo principale
				 */
				for (IMethod meth : methods) {
					//Effettuo la pulizia del metodo della classe in utilizzo
					for (IMethod meth_dis: methods_discover) {
						//Effettuo la pulizia del metodo
						//String secondo = cleanMethod(meth_dis);
						if (meth.getElementName().equals(meth_dis.getElementName())) {
							//System.out.println("Uguali: " + meth.getElementName() + " e " + meth_dis.getElementName());
							/*
							 * Richiamo il metodo che permette in primis di pulire i due metodi e poi di fare un confronto sulla loro forma
							 * e la loro struttura.
							 */
							this.n_metodi_uguali++;
							boolean ugugaglianza = cleanAndMatch(meth, meth_dis);
							if (ugugaglianza == false) {
								/*
								System.out.println("I due metodi: " + meth.getElementName() + " e "
										+ meth_dis.getElementName() + " non risultano essere uguali");

								 */

								//Trovata discrepenza
								check_methods = false;
							} else {
								/*
								System.out.println("I due metodi: " + meth.getElementName() + " e "
										+ meth_dis.getElementName() + " risultano essere uguali");
								 */

							}
						}
					}
				}

				//Prima di passare al prossimo tipo
				if (check_methods == true) {
					val_inh_spec++;
				}
			}
		} else {
			//System.out.println("Non ci sono classi da estendere");
			//Verifico se ci sono però interfacce di riferimento
			if (all_interface.size() > 0) {
				String[] interfacce = type.getSuperInterfaceNames();
				for (String c : interfacce) {
					all_interface.add(c);
				}
				val_inh_spec = all_interface.size();
			} else {
				//System.out.println("Non ci sono interfacce estese");
				val_inh_spec = 0;
			} 
		}
		return val_inh_spec;
	}

	/*
	 *
	 *									METODI D'APPOGGIO UTILI PER ASSISTERE I METODI CREATI
	 *								    PER IL CALCOLO DELL'EREDITARIETA' DI IMPLEMENTAZIONE E
	 *												DELL'EREDITARIETA' DI SPECIFICA
	 *
	 *
	 */

	/*
	 * Metodo che permette di fare la pulizia di un metodo dai commenti
	 */
	public String cleanMethod (IMethod meth) throws JavaModelException {
		String sorgente = meth.getSource();
		String[] sorg_meth = sorgente.split("\n");

		String metodo = "";

		//ArrayList fondamentale dopo la pulizia del metodo
		ArrayList<String> stringhe_uno = new ArrayList<String>();

		for (String s : sorg_meth) {
			//Elimino gli spazi bianchi
			s = s.trim();
			//Verifico se il primo carattere risulta essere un commento
			if (!(s.startsWith("*") || s.startsWith("/*") || s.startsWith("//"))) {
				//Ulteriore verifica se è presente un commento interno della linea in esame
				this.LOC++;
				String[] in_lines = s.split("//");
				stringhe_uno.add(in_lines[0]);
			}
		}

		for (String s : stringhe_uno) {
			metodo += s;
		}

		return metodo;
	}

	/*
	 * Metodo che permette di fare la pulizia di un metodo dai commenti
	 */
	public String cleanMethodnoSignature (IMethod meth) throws JavaModelException {
		String sorgente = meth.getSource();
		String[] sorg_meth = sorgente.split("\n");
		
		String metodo = "";

		//ArrayList fondamentale dopo la pulizia del metodo
		ArrayList<String> stringhe_uno = new ArrayList<String>();

		for (String s : sorg_meth) {
			//Elimino gli spazi bianchi
			s = s.trim();
			//Verifico se il primo carattere risulta essere un commento
			if (!(s.startsWith("*") || s.startsWith("/*") || s.startsWith("//"))) {
				//Ulteriore verifica se è presente un commento interno della linea in esame
				//this.LOC++;
				String[] in_lines = s.split("//");
				stringhe_uno.add(in_lines[0]);
			}
		}
		
		stringhe_uno.remove(0);

		for (String s : stringhe_uno) {
			metodo += s;
		}

		return metodo;
	}

	/*
	 * Metodo che permette di effettuare la pulizia del codice, prende in input un metodo e ne effettua la pulizia
	 * da tutti i tipi di commenti, si quelli posti all'inizio del metodo, sia quelli interni.
	 * Il metodo ovviamente tornerà un intero che rappresenterà solo il codice sorgente del metodo
	 */
	public boolean cleanAndMatch (IMethod meth, IMethod meth2) throws JavaModelException {
		//Prendo il codice sorgente del primo metodo
		String lines_uno = meth.getSource();
		//Prendo il codice sorgente del secondo metodo
		String lines_two = meth2.getSource();
		//Effettuo lo split su ogni punto e da capo del primo metodo
		String[] metodo_uno = lines_uno.split("\n");
		//Effettuo lo split su ogni punto e da capo del secondo metodo
		String[] metodo_due = lines_two.split("\n");

		//Stringa del primo metodo per fare il confronto finale
		String meth_uno = "";
		//Stringa del secondo metodo per fare il confronto finale
		String meth_due = "";

		//ArrayList per la gestione delle stringhe compatibile e che non sono commento
		ArrayList<String> stringhe_uno = new ArrayList<String>();
		//ArrayList per la gestione delle stringhe compatibili e che non sono un commento
		ArrayList<String> stringhe_due = new ArrayList<String>();

		//For che mi permette di analizzare ogni lines del primo metodo
		for (String s : metodo_uno) { 
			//Elimino gli spazi bianchi
			s = s.trim();
			//Verifico se il primo carattere risulta essere un commento
			if (!(s.startsWith("*") || s.startsWith("/*") || s.startsWith("//"))) {
				//Ulteriore verifica se è presente un commento interno della linea in esame
				String[] in_lines = s.split("//");
				stringhe_uno.add(in_lines[0]);
			}
		}

		//For che mi permette di analizzare ogni lines del secondo metodo
		for (String s : metodo_due) { 
			//Elimino gli spazi bianchi
			s = s.trim();
			//Verifico se il primo carattere risulta essere un commento
			if (!(s.startsWith("*") || s.startsWith("/*") || s.startsWith("//"))) {
				//Ulteriore verifica se è presente un commento interno della linea in esame
				String[] in_lines = s.split("//");
				stringhe_due.add(in_lines[0]);
			}
		}

		//For per riempire la stringa inerente al primo metodo
		for (String s : stringhe_uno) {
			meth_uno += s;
		}

		//For per riempire la stringa inerente al secondo metodo
		for (String s : stringhe_due) {
			meth_due += s;
		}

		/*
		System.out.println("Stampo il metodo uno in esame: ");
		System.out.println(meth_uno);
		System.out.println("Stampo il metodo due in esame: ");
		System.out.println(meth_due);
		 */
		if (meth_uno.equals(meth_due)) {
			return true;
		}else {
			return false;
		}
	}

	/*
	 * Metodo importante che permette da una variabiel di appoggio che descrive il punto di estensione di una classe
	 * estrapolare il tipo associato nel progetto in esame e dal tipo estrapolato andare ad estrapolare il nuovo punto
	 * di estensione sotto parola chiave extends
	 */
	public String new_Classes(ArrayList<IType> types) throws JavaModelException{
		//Verifico se la Stringa prensete in appoggio corrisponde ad un tipo del progetto
		//Boolean per l'uscita dal metodo
		boolean uscita = false;
		for (IType t : types) {
			if (t.getElementName().equals(appoggio)) {
				uscita = true;
				//Se si trova una corrispondenza allora estrapolo eventuale punto di estensione sotto extends
				String classe_estesa = t.getSuperclassName();
				//Se la classe_estesa è nulla allora set di appoggio al valore di uscita del while
				if (classe_estesa == null) {
					this.appoggio = "exit";
				} else {
					//Altrimenti solito controllo se nell'array è presente la classe estesa
					if (!(all_extends.contains(classe_estesa))) {
						all_extends.add(classe_estesa);
						appoggio = classe_estesa;
					}
				}
			}
		}
		if (uscita == false) {
			this.appoggio = "exit";
		}
		return appoggio;
	}

	/*
	 *
	 *									METODI IMPORTANTI PER DEFINIRE NUOVE METRICHE PER
	 *									IL CALCOLO DI NUOVE METRICHE A LIVELLO DI TIPO
	 *									CHE POSSONO ESSERE FINDAMENTALI PER LA SUCCESSIVA ANALISI
	 *
	 *
	 */

	/*
	 * Metodo che permette di calcolare per tutte le interfacce il loro livello di profondità nell'albero
	 * gerarchico
	 * @param interfaces -> Insieme di tutte le interfacce del progetto
	 * @param types -> Insieme di tutti i tipi del progetto
	 */
	public void calcolaInhForInterfaces (ArrayList<IType> interfaces, ArrayList<IType> types) throws JavaModelException {
		//In primis per ogni interfaccia vado ad estrapolare le interfacce che possono essere estese
		for (IType type : interfaces) {
			DitInheritance dit = new DitInheritance();
			ArrayList<String> punti_estensione = new ArrayList<String>();
			int level = 0;

			String[] estensioni = type.getSuperInterfaceNames();
			if (estensioni != null) {
				//Aggiungo le interfacce al mio ArrayList
				for (String s : estensioni) {
					punti_estensione.add(s);
				}
				while (punti_estensione.size() > 0) {
					level++;
					//System.out.println("Chiamo il metodo...");
					punti_estensione = calcola_nuovo_livello(punti_estensione, interfaces);
				}
			}
			dit.setName_interface(type.getElementName());
			dit.setLevel(level);
			inhInter.add(dit);
		}
		//Stampa dei risultati ottenuti
		/*
		for (DitInheritance dit : inhInter) {
			System.out.println("Interfaccia: " + dit.getName_interface() + " con valore del dit: " + dit.getLevel());
		}
		 */
	}

	/*
	 * Metodo richiamato dal metodo calcolaInhForInterfaces per andare ad estrapolare le interfacce vincolanti
	 * ad un determinato tipo, conservando anche il livello di appartenenza
	 */
	public ArrayList<String> calcola_nuovo_livello(ArrayList<String> estensioni, ArrayList<IType> interfacce) throws JavaModelException{
		//Variabile di ritorno
		ArrayList<String> di_ritorno = new ArrayList<String>();
		for (String s : estensioni) {
			for (IType type : interfacce) {
				if (s.equals(type.getElementName())) {
					String[] inter = type.getSuperInterfaceNames();
					if (inter != null) {
						for (String stringa : inter) {
							di_ritorno.add(stringa);
						}
					}
				}
			}
		}
		return di_ritorno;
	}

	/*
	 * Metodo che permette di calcolare prima del calcolo definitivo del dit, per il tipo passato come parametro
	 * tutte le classe alle quali dipende sotto parola chiave extends
	 */
	public void calcola_dipendenzeDIT (IType type, ArrayList<IType> types) throws JavaModelException {
		//Inizializzo l'array vincolante a tutte le classi estese per la classe in esame
		this.inhFType = new ArrayList<DitInheritance>();
		//Variabile per calcolare il livello
		int level = 0;
		DitInheritance dit_inh = new DitInheritance();
		dit_inh.setName_interface(type.getElementName());
		dit_inh.setLevel(level);
		inhFType.add(dit_inh);
		//Estrapolo la superclasse di primo livello per il tipo in esame
		String estensione = type.getSuperclassName();
		//System.out.println("Ecco l'estensione da valutare: " + estensione);
		if (estensione != null) {
			while (estensione != null) {
				level++;
				//System.out.println("Livello: " + level);
				//Ricerco la classe nei tipi del progetto
				String app = estensione;
				boolean check = false;
				for (IType t : types) {
					if (t.getElementName().equals(app)) {
						DitInheritance ditRif = new DitInheritance();
						ditRif.setName_interface(estensione);
						ditRif.setLevel(level);
						inhFType.add(ditRif);
						estensione = t.getSuperclassName();
						check = true;
					}
				}
				if (check == false) {
					DitInheritance dit_senza_tipo = new DitInheritance();
					dit_senza_tipo.setName_interface(estensione);
					dit_senza_tipo.setLevel(level);
					inhFType.add(dit_senza_tipo);
					estensione = null;
				}
			}
		}
		/*
		 * For finale per la stampa di tutte le estensione ritrovate con il rispettivo livello
		 */
		/*
		for (DitInheritance ss : inhFType) {
			System.out.println("classe: " + ss.getName_interface() + ", con livello: " + ss.getLevel());
		}
		 */
		/*
		 * A questo punto del metodo andiamo a definire il dit per il parametro type del metodo in esame
		 */
		//Definiamo varibaile massima che descrive il DIT. Un valore sicuro è sicuramente pari al numero di estensioni ritrovate
		//Prendo l'array delle dipendenze extends estrapolo l'ultima occorrenze e prendo il livello massimo
		int max = 0;
		for (DitInheritance d : inhFType) {
			if (max < d.getLevel()) {
				max = d.getLevel();
			}
		}

		for (DitInheritance d : inhFType) {
			//Prendo il tipo di riferimento
			for (IType t : types) {
				if (t.getElementName().equals(d.getName_interface())) {
					//Estrapolo le interfacce
					String[] interfacce = t.getSuperInterfaceNames();
					//Verifico il livello di ogni intefaccia dopo avere accertato la presenza nell'array inhIInter
					for (DitInheritance inter : inhInter) {
						for (String s : interfacce) {
							if (s.equals(inter.getName_interface())) {
								//Valore ereditarietà interfaccia il più uno perchè l'interfaccia per la classe in esame è un nuovo livello
								int livello = inter.getLevel() + d.getLevel() + 1;
								if (max < livello) {
									//Nuovo massimo
									max = livello;
								}
							}
						}
					}
				}
			}
		}
		this.max_dit = max;
		//Stampa per il DIT
		//System.out.println("Per la classe type: " + type.getElementName() +  ", il valore del DIT è pari a: " + max);
	}

	@Override
	public int getCompilerFlags() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLineQualifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPattern() {
		// TODO Auto-generated method stub
		return pattern;
	}

	@Override
	public void run(IAction action) {
		System.out.println("METODO FONDAMENTALE...");
		// Get the root of the workspace
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();

		for (IProject project : projects) {
			try {
				if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
					IJavaProject java_project = JavaCore.create(project);
					IPackageFragment[] packages = java_project.getPackageFragments();

					for (IPackageFragment pacchetto : packages) {
						if (pacchetto.getKind() == IPackageFragmentRoot.K_SOURCE) {
							for (ICompilationUnit cu : pacchetto.getCompilationUnits()) {
								System.out.println("Nome della classe esaminata: " + cu.getElementName());
							}
						}
					}
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Loop over all projects
		for (IProject project : projects) {
			System.out.println(project.getName());
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub
		this.window = window;
	}

	/*
	 *
	 *									METODI FONDAMENTALI PER LEGGERE DAI FILE .CSV PRODOTTI DA METRICS
	 *									E POI ANDARLI A SCRIVERE INSIEME ALLE METRICHE DA ME PRODOTTO
	 *
	 *
	 */

	public void read_csv_Metrics (ArrayList<String> alltypes) throws IOException {
		System.out.println("Provo a leggete il documento");
		//Prendo riferimento al path del csv estrapolato ed espertota da Metrics
		String path = "C:\\Users\\anton\\OneDrive\\Desktop\\MetricsTesi\\Metrics" + this.nome_progetto + ".csv";
		//Definisco un Buffer Reader per la lettura del documento
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String line = "";
		int i = 0;
		//Ciclo con un while finchè non incontro una linea = null
		int granularità = 0;
		int id_metrica = 0;
		int index_name_type = 0;
		int index_value = 0;
		while ((line = reader.readLine()) != null) {
			i++;
			//Tramite il metodo split divido la stringa in esame in relazione al ;
			String[] occorrenze = line.split(";");
			ArrayList<String> utilità = new ArrayList<String>();
			if (i == 1) {
				int j = 0;
				for (String s: occorrenze) {
					if (s.equals("per")) {
						System.out.println("Assegno granularità: " + j);
						granularità = j;
					}
					if (s.equals("id")) {
						System.out.println("Assegno granularità: " + j);
						id_metrica = j;
					}
					if (s.equals("name")) {
						System.out.println("Assegno granularità: " + j);
						index_name_type = j;
					}
					if (s.equals("value")) {
						System.out.println("Assegno granularità: " + j);
						index_value = j;
					}
					utilità.add(s);
					System.out.println("Valore: " + j + " :" + s);
					j++;
				}
			}
			/*
			 * Variabile che permette di valutare e definire la granularità della metrica che noi dobbiamo considerare
			 * solo quelle di livello type
			 */
			String level_granularità = occorrenze[granularità];
			if (level_granularità.equals("type")) {
				//Stiamo considerando una metrica di tipo, controllo di quale metrica parliamo
				String nome_metrica = occorrenze[id_metrica];
				switch (nome_metrica) {
				//Caso di riferimento relativo al calcolo del DIT
				case "DIT":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_tipo = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_tipo)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_tipo)) {
								metrica.setDit_metrics(Integer.parseInt(occorrenze[index_value]));
								System.out.println("Valore del DIT: " + occorrenze[index_value]);
							}
						}
					}
					break;

					//Metrica che definisce i numero di metodi pesati all'interno di una classe in esame
				case "WMC":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_wmc = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_wmc)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_wmc)) {
								metrica.setWmc(Integer.parseInt(occorrenze[index_value]));
							}
						}
					}
					break;

					//Metrica che riguarda il number of children, in definitiva il noc per la metrica
				case "NSC":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nsc = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nsc)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nsc)) {
								metrica.setNoc(Integer.parseInt(occorrenze[index_value]));
							}
						}
					}
					break;

					//Metrica che riguarda il norm, cioè il numero di metodi sovrascritti dalla classe
				case "NORM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_norm = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_norm)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_norm)) {
								metrica.setNorm(Integer.parseInt(occorrenze[index_value]));
							}
						}
					}
					break;

					//Metrica LCOM, che rappresenta il Lack of Cohesion relativa alla classe in esame
				case "LCOM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_lcom = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_lcom)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_lcom)) {
								String valore = occorrenze[index_value].replace(",", ".");
								metrica.setLcom(Double.parseDouble(valore));
							}
						}
					}
					break;

					//Metrica che permette di definire per ogni tipo il numero di attributi presenti
				case "NOF":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_na = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_na)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_na)) {
								metrica.setNa(Integer.parseInt(occorrenze[index_value]));
							}
						}
					}
					break;

					//Metrica che ritorna l'insieme di tutti gli attributi statici vincolanti all'interno della classe
				case "NSF":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nsa = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nsa)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nsa)) {
								metrica.setNsf(Integer.parseInt(occorrenze[index_value]));
							}
						}
					}
					break;

					//Metrica che ritorna l'insieme di tutti i metodi vincolanti all'interno della classe
				case "NOM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nom = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nom)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nom)) {
								metrica.setNom(Integer.parseInt(occorrenze[index_value]));
							}
						}
					}
					break;

					//Calcolo del numero di metodi che risultano essere statici
				case "NSM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nms = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nms)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nms)) {
								metrica.setNsm(Integer.parseInt(occorrenze[index_value]));
							}
						}
					}
					break;

					//Indice di specializzazione della classe
				case "SIX":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_is = occorrenze[index_name_type];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_is)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_is)) {
								String valore = occorrenze[index_value].replace(",", ".");
								metrica.setSix(Double.parseDouble(valore));
							}
						}
					}
					break;
				default:
					break;
				}

			}
		}
	}

	/*
	 * Partiamo nell'esaminare il metodo per andare a leggere le metriche estrapolate da METRICS file .csv
	 * @param -> un semplice array che contiene solo i nomi di tutti i tipi che sono vincolanti all'interno del progetto
	 */
	public void read_CSV_Metrics (ArrayList<String> alltypes) throws IOException {
		System.out.println("Provo a leggete il documento");
		//Prendo riferimento al path del csv estrapolato ed espertota da Metrics
		String path = "C:\\Users\\anton\\OneDrive\\Desktop\\MetricsTesi\\Metrics" + this.nome_progetto + ".csv";
		//Definisco un Buffer Reader per la lettura del documento
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String line = "";
		//Ciclo con un while finchè non incontro una linea = null
		while ((line = reader.readLine()) != null) {
			//Tramite il metodo split divido la stringa in esame in relazione al ;
			String[] occorrenze = line.split(";");
			/*
			 * Variabile che permette di valutare e definire la granularità della metrica che noi dobbiamo considerare
			 * solo quelle di livello type
			 */
			String level_granularità = occorrenze[5];
			if (level_granularità.contentEquals("type")) {
				//Stiamo considerando una metrica di tipo, controllo di quale metrica parliamo
				String nome_metrica = occorrenze[3];
				switch (nome_metrica) {
				//Caso di riferimento relativo al calcolo del DIT
				case "DIT":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_tipo = occorrenze[9];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_tipo)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_tipo)) {
								metrica.setDit_metrics(Integer.parseInt(occorrenze[12]));
							}
						}
					}
					break;

					//Metrica che definisce i numero di metodi pesati all'interno di una classe in esame
				case "WMC":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_wmc = occorrenze[10];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_wmc)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_wmc)) {
								metrica.setWmc(Integer.parseInt(occorrenze[13]));
								System.out.println("Valore assegnato di WMC");
							}
						}
					}
					break;

					//Metrica che riguarda il number of children, in definitiva il noc per la metrica
				case "NSC":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nsc = occorrenze[10];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nsc)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nsc)) {
								metrica.setNoc(Integer.parseInt(occorrenze[13]));
							}
						}
					}
					break;

					//Metrica che riguarda il norm, cioè il numero di metodi sovrascritti dalla classe
				case "NORM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_norm = occorrenze[10];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_norm)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_norm)) {
								metrica.setNorm(Integer.parseInt(occorrenze[13]));
							}
						}
					}
					break;

					//Metrica LCOM, che rappresenta il Lack of Cohesion relativa alla classe in esame
				case "LCOM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_lcom = occorrenze[9];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_lcom)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_lcom)) {
								String valore = occorrenze[12].replace(",", ".");
								metrica.setLcom(Double.parseDouble(valore));
							}
						}
					}
					break;

					//Metrica che permette di definire per ogni tipo il numero di attributi presenti
				case "NOF":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_na = occorrenze[10];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_na)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_na)) {
								metrica.setNa(Integer.parseInt(occorrenze[13]));
							}
						}
					}
					break;

					//Metrica che ritorna l'insieme di tutti gli attributi statici vincolanti all'interno della classe
				case "NSF":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nsa = occorrenze[10];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nsa)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nsa)) {
								metrica.setNsf(Integer.parseInt(occorrenze[13]));
							}
						}
					}
					break;

					//Metrica che ritorna l'insieme di tutti i metodi vincolanti all'interno della classe
				case "NOM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nom = occorrenze[10];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nom)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nom)) {
								metrica.setNom(Integer.parseInt(occorrenze[13]));
							}
						}
					}
					break;

					//Calcolo del numero di metodi che risultano essere statici
				case "NSM":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_nms = occorrenze[10];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_nms)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_nms)) {
								metrica.setNsm(Integer.parseInt(occorrenze[13]));
							}
						}
					}
					break;

					//Indice di specializzazione della classe
				case "SIX":
					//Istanzio un nuovo riferimento in quanto prima metrica del dataset
					String nome_is = occorrenze[9];
					//Verifico se il tipo ritornato è tra quello presenti del progetto
					if (alltypes.contains(nome_is)) {
						//Definisco una nuova occorrenza di metrica
						for (TypeMetrics metrica: all_metrics) {
							if (metrica.getNome_tipo().equals(nome_is)) {
								String valore = occorrenze[12].replace(",", ".");
								metrica.setSix(Double.parseDouble(valore));
							}
						}
					}
					break;
				default:
					break;
				}

			}
		}
	}

	public void write_CSV (ArrayList<TypeMetrics> all_metrics) throws IOException {
		//Definisco il path di dove salvare il documento
		String path_return = "C:\\Users\\anton\\OneDrive\\Desktop\\MetricsTesi\\Metriche esportate\\JHotDraw\\JHotDrawallVersions\\" + nome_progetto + ".csv";
		//Definisco il mio FileWriter
		FileWriter writer = new FileWriter(path_return);
		//Definisco il separatore del documento
		String separator = ";";
		//For che riguarda le colonne
		String[] columns = {"Nome del progetto", "Nome del tipo", "DIT", "WMC", "NOC", "LCOM", "CBO", "RFC", "NOM", "NA", "Delegation", "Delegation2", "Inh_impl", "nMO", "Inh_spec", "LOC", "NORM", "NSF", "NSM", "SIX"};

		for (int index = 0; index < columns.length; index++) {
			writer.append(columns[index]);
			if (index < columns.length-1) {
				writer.append(separator);
			}
		}

		//Aggiungo linea di spazio
		writer.append(System.lineSeparator());

		//Adesso aggiungo tutti riferimenti nel dataset
		for (TypeMetrics t : all_metrics) {
			writer.append(t.getProject_name());
			writer.append(separator);
			writer.append(t.getNome_tipo());
			writer.append(separator);
			writer.append(""+t.getDit_metrics());
			writer.append(separator);
			writer.append(""+t.getWmc());
			writer.append(separator);
			writer.append("" + t.getNoc());
			writer.append(separator);
			writer.append("" + t.getLcom());
			writer.append(separator);
			writer.append("" + t.getCbo());
			writer.append(separator);
			writer.append("" + t.getRfc());
			writer.append(separator);
			writer.append("" + t.getNom());
			writer.append(separator);
			writer.append("" + t.getNa());
			writer.append(separator);
			writer.append("" + t.getDelegation());
			writer.append(separator);
			writer.append("" + t.getDelegation_two());
			writer.append(separator);
			writer.append("" + t.getInh_impl());
			writer.append(separator);
			writer.append("" + t.getN_methods());
			writer.append(separator);
			writer.append("" + t.getInh_spec());
			writer.append(separator);
			writer.append("" + t.getLOC());
			writer.append(separator);
			writer.append("" + t.getNorm());
			writer.append(separator);
			writer.append("" + t.getNsf());
			writer.append(separator);
			writer.append("" + t.getNsm());
			writer.append(separator);
			writer.append("" + t.getSix());
			writer.append(System.lineSeparator());
		}
		writer.flush();
		writer.close();
	}

	/*
	 *
	 *
	 *									METODI BETA PER IL CALCOLO DELL'EREDITARIETA'
	 *									DI SPECIFICA E EREDITARIETA' DI IMPLEMENTAZIONE
	 *
	 *
	 */
	/*
	 * Metodo che calcola lo score dell'ereditarietà di implementazione relativo a un determinato tipo partendo solo da una determinata classe fissata a priori
	 * @param type -> tipo in esame sul quale calcolare lo score;
	 * @param types -> lista di tutti i tipi vincolanti a un determinato progetto
	 * @param methods -> lista di tutti i metodi vincolanti al tipo in esame type
	 */
	public int calcola_Inh_Impl (IType type, ArrayList<IType> types, IMethod[] methods) throws JavaModelException {
		//Visto che non riesco ad utilizzare contains con due tipi di array differente, devo adattare IType in String
		ArrayList<String> types_proj = new ArrayList<String>();
		//Variabile per quantificare ereditarietà di implementazione e variabile di ritorno del metodo
		int inh_impl = 0;
		//Modo per ritornare tutti i campi dichiarati all'interno del tipo in esame (insieme di tutti i tipi)
		IField[] fields = type.getFields();

		if (type.getElementName().equals("DrawApplet")) {
			System.out.println("Stampa di tutti i tipi del progetto: ");
			for (IType tipo : types) {
				types_proj.add(tipo.getElementName());
				System.out.println(tipo.getElementName());
			}
			//Istanzio ArrayList che contiene tutti i tipi dei campi del tipo da analizzare
			ArrayList<String> all_types = new ArrayList<String>();
			/*
			 * Per ogni variabili di istanza della classe verifico se il tipo del campo combacia con uno dei tipi
			 * presenti nel progetti e definiti dal programmatore stesso
			 */
			int contatore_Inh_Impl = 0;
			for (IField field: fields) {
				//Ottengo il riferimento al tipo del campo
				String type_field = field.getTypeSignature().toString();
				//Controllo se si tratta di un tipo basilare di Eclipse (int, double, boolean, etc...)
				if (!(type_field.length() == 1)) {
					//Se non è uguale ad uno la lunghezza prendo il riferimento del tipo del campo (variabile d'istanza)
					type_field = type_field.substring(1, type_field.length()-1);
					//Stampa tutti i tipi delle variabili di istanza in esame
					System.out.println("Tipo istanza: " + type_field);
					//Controllo per evitare di considerare tipi String, che non sono oggetto del nostro studio
					if (!(type_field.equals("String"))) {
						//Aggiungo il tipo all'array di tipi precedentemente istanziato
						all_types.add(type_field);
						//Output per verificare nome della variabile di istanza + tipo della variabile stessa.
						//System.out.println("Variabile di istanza: " + field.getElementName() + ", con il seguente tipo: " + type_field);
						/*
						 * A questo punto dobbiamo andare a verificare se il tipo in esame appartiene ad un tipo 
						 * presente nel parametro types, i quali rappresentano tutti i tipi definiti nel progetto dal 
						 * programmatore stesso. Se nell'analisi si volesse andare a considerare anche i tipi provenienti
						 * da sistemi esterno o da librerie utilizzate, a questo punto basterà togliere questo tipo di controllo
						 */
						boolean check_control = false;
						//Istruzione utile per verificare la presenza di un eventuale match (tipo istanza - tipo del progetto)
						check_control = types_proj.contains(type_field);
						if (check_control == true) {
							/*
							 * Istruzioni contatore e stampa che mi sono servite per verificare manualmente il numero di riscontri ottenuti
							 * e mostrare in console che tipo di match si è verificato
							 */
							//contatore++;
							//System.out.println("Match rilevato: " + type_field + " num:" + contatore);
							/*
							 * Dopo aver trovato correttamente il match tra il tipo della variabile di istanza con un generico
							 * tipo del progetto, istanziato e progettato dal programmatore, a questo punto dobbiamo dobbiamo verificare
							 * che il field in esame sia effettivamente utilizzato nei metodi della classe in esame. La lista dei metodi
							 * è presente come parametro del metodo in un ArrayList di Methods
							 */
							for (IMethod method: methods) {
								/*
								 * Istruzioni per mostrare a video il codice sorgente del metodo in esame
								 */
								//System.out.println("Stampo il codice sorgente di tutti i metodi: ");
								//System.out.println(method.getSource());
								String variabile = method.getSource();
								boolean verify = variabile.contains(field.getElementName() + ".");
								if (verify == true) {
									System.out.println("Ho trovato un utilizzo della variabile: " + field.getElementName());
									System.out.println("L'ho trovato nel metodo con nome: " + method.getElementName());
									contatore_Inh_Impl++;
								}
							}
						}
					}
				}
			}
			System.out.println("Valore di implementation_inheritance per DrawApplet: " + contatore_Inh_Impl);
			inh_impl = contatore_Inh_Impl;
		}
		return inh_impl;
	}

	/*
	 * Metodo che permette di calcolare il valore di ereditarietà di specifica fissando a priori una classe di riferimento,
	 * ciò permette in maniera semplice di poter verificare l'output di ogni singola istruzione ritornata dal metodo stesso.
	 * Il progetto in esame è JHotDraw51, con la classe di riferimento DrawApplet.
	 * I parametri del metodo sono
	 * @param type -> tipo della classe sulla quale calcolare l'ereditarietà di specifica
	 * @param types -> ArrayList che mantiene tutti i tipi vincolanti nel progetto in esame
	 * @param methods -> Paramentro che conserva tutti i metodi del paramentro type passato
	 */
	public int calcola_Inh_Spec(IType type, ArrayList<IType> types, IMethod[] methods) throws JavaModelException {
		//Istanzio la variabile di ritorno
		int val_inh_spec = 0;
		this.appoggio = null;
		//Istanzio ArrayList per memorizzare tutte le interfacce riscontrate
		this.all_interface = new ArrayList<String>();
		//Istanzio ArrayList per memorizzare tutte le estensioni riscontrate
		this.all_extends = new ArrayList<String>();
		//ArrayList di tipi ricercati utile per non ripetere sempre gli stessi calcoli
		ArrayList<IType> types_discover = new ArrayList<IType>();
		/*
		 * Prima prova con JDT Core proviamo con getSuperClassName di ottenere la classe sotto la parola chiave extends,
		 * mentre con il metodo getSuperInterfaceNames, cerchiamo di ottenere tutte le interfacce implementato nel tipo in
		 * esame.
		 */
		//Metodo vincolante solo alla classe DrawApplet
		if (type.getElementName().equals("BouncingDrawing")) {
			//Riferimento alla superclasse con parola chiave extends
			String classe_estesa = type.getSuperclassName();

			//Verifico se ci sono classe estese
			if (!(classe_estesa == null)) {
				//Se esiste parola chiave extends aggiungo all'array di tutte le estensioni
				all_extends.add(classe_estesa);
				//Stringa di appoggio per gestire la chiamata ricorsiva
				//Riferimento alla superclasse con parola chiave implements
				String[] interface_implementate = type.getSuperInterfaceNames();
				if (!(interface_implementate == null)) {
					for (String c : interface_implementate) {
						this.all_interface.add(c);
						//System.out.println("Implemento la mia interfaccia: " + c);
					}
				}
				this.appoggio = classe_estesa;

				//Variabile che mi permette di gestire il ciclo while successivo
				boolean check = true;

				//finchè check non è false
				while (check) {
					/*
					 * In appoggio (riferimeno alla classe con parola chiave extends del tipo che in quel momento 
					 * risulta essere presente nella stringa appoggio).
					 * Il metodo new_Classes non fa altro che ritrovare IType di riferimento per la variabile appoggio
					 * e a quel punto estrapolare un eventuale tipo sotto parola chiave extends
					 */
					appoggio = new_Classes (types);
					//Controllo se il risultato risulta essere di uscita
					if (appoggio == "exit") {
						//Esco dal while
						check = false;
					} else {
						//Ho riscontrato una nuova estensione sotto parola chiave extends
						//Prima di aggiungere la classe verifico se essa già è presente nell'array che contiene tutte le estensioni
						if (!(all_extends.contains(appoggio))) {
							//System.out.println("Aggiungo la classe: ");
							all_extends.add(appoggio);
						}
					} 
				}

				/*
				 * Verifico corrispondenza di ogni tipo con ogni stringa prensente in all extends
				 * Inoltre con questo for permetto di aggiungere tutte le interfaccie vincolanti alle classi
				 * estese con punto chiave extends di una determinata classe scoperta.
				 */
				for (IType t : types) {
					for (String s : all_extends) {
						if (t.getElementName().contentEquals(s)) {
							//Aggiungo la classe ricercata come tipo all'array inzialmente istanziato
							types_discover.add(t);
							// Se si trova corrispondeza strampo tutte le interfacce del tipo in esame, sotto implements
							String[] interfacce_nuove = t.getSuperInterfaceNames();
							//Aggiungo a all_interface solo le nuove interfacce, non quelle già scoperte
							for (String nuova: interfacce_nuove) {
								if (!(all_interface.contains(nuova))) {
									all_interface.add(nuova);
								}
							}
						}
					}
				}

				/*
				 * Incremento il valore associato all'ereditarietà di specifica per la classe in esame, sommando
				 * al valore di ritorno il numero di tutte le interfaccie ritornate.
				 */
				val_inh_spec = val_inh_spec + all_interface.size();
				//System.out.println("Valore delle interfacce: " + all_interface.size());

				//For per ciclare tutti i tipi delle classi estese
				for (IType t : types_discover) {
					//Estrapolo tutti i metodi del tipo scoperto
					IMethod[] methods_discover = t.getMethods();
					//Variabile che mi fa capire se per il tipo in esame esiste una discrepanza
					boolean check_methods = true;
					/*
					 * Doppio for dove:
					 * - Il primo permette di ciclare i metodi della classe estendente
					 * - Il secondo permette di ciclare i metodi e rappresenta un parametro del metodo principale
					 */
					for (IMethod meth : methods) {
						//Effettuo la pulizio del metodo della classe in utilizzo
						for (IMethod meth_dis: methods_discover) {
							//Effettuo la pulizia del metodo
							//String secondo = cleanMethod(meth_dis);
							if (meth.getElementName().equals(meth_dis.getElementName())) {
								//System.out.println("Uguali: " + meth.getElementName() + " e " + meth_dis.getElementName());
								/*
								 * Richiamo il metodo che permette in primis di pulire i due metodi e poi di fare un confronto sulla loro forma
								 * e la loro struttura.
								 */
								boolean ugugaglianza = cleanAndMatch(meth, meth_dis);
								if (ugugaglianza == false) {

									System.out.println("I due metodi: " + meth.getElementName() + " e "
											+ meth_dis.getElementName() + " non risultano essere uguali");

									//Trovata discrepenza
									check_methods = false;
								} else {

									System.out.println("I due metodi: " + meth.getElementName() + " e "
											+ meth_dis.getElementName() + " risultano essere uguali");

								}
							}
						}
					}


					//Prima di passare al prossimo tipo
					if (check_methods == true) {
						/*
						 * Nota se non viene ritrovato riscontro in nessun metodo allora il check methods 
						 * risulterà essere positivo, quindi senza errore verrà incrementato il livello associato all'ereditarietà di specifica
						 */
						//Non si sono osservate discrepanze incremento il valore dell'ereditarietà di specifica
						//System.out.println("Sono check methods con valore: " + check_methods);
						val_inh_spec++;
					}
				}

				/*
				 * Alcuni elementi di stampa utili per poter stampare solo le interfaccie vincolanti per la classe in esame
				 * quindi controllo attivabili solo quando si vogliono visualizzare in console le informazioni.
				if (all_interface.size() > 0) {
					for (String c : all_interface) {
						System.out.println("Interfaccia: " + c);
					}
				} else {
					System.out.println("Non ci sono interfacce estese");
				}

				//Stampo tutte le classi estese
				for (String s : all_extends) {
					System.out.println("Classe estesa: " + s);
				}
				 */
			} else {
				System.out.println("Non ci sono classi da estendere");
				//Verifico se ci sono però interfacce di riferimento
				if (all_interface.size()>0) {
					String[] interfacce = type.getSuperInterfaceNames();
					for (String c : interfacce) {
						all_interface.add(c);
					}
					val_inh_spec = all_interface.size();
				} else {
					System.out.println("Non ci sono interfacce estese");
					val_inh_spec = 0;
				} 
			}
			System.out.println("Il valore dell'ereditarietà di specifica è : " + val_inh_spec);
		}
		return val_inh_spec;
	}

	/*
	 * 
	 * METODO NON PIU' UTILIZZATO
	 * 
	 */
	/*
	 * Metodo che permette di calcolare la metrica DIT, che calcola il livello di profondità del tipo in esame nella
	 * sua struttura gerarchica
	 * @param type -> tipo sul quale definire il valore del DIT
	 * @param types -> elenco di tutti i tipi presenti nel progetto in esame
	 * @param interfaces -> lista di tutte le interfacce vincolanti nel progetto
	 */
	public int calcola_DIT (IType type, ArrayList<IType> types) throws JavaModelException {
		int ritorno = 0;
		/*
		 * Dopo aver definito quello che è il livello di ereditarietà per tutte le interfaccce del progetto
		 * andiamo a questo punto a definire in che modo possiamo andare a calcolare il valore di ereditarietà
		 * per ogni singolo tipo, in funzione anche alle interfacce che sono state ritornate e reperite
		 */
		//String in primis estrapolo il nome del tipo esteso sotto parola chiave extends
		String estensione = type.getSuperclassName();
		dit_for_Interfaces(type.getElementName(), types);
		//Variabile che step by step definisce il valore massimo del dit per la classe in esame
		this.max_dit = 0;
		this.level = 0;
		if (type.getElementName().equals("DrawApplet")) {
			if (estensione != null) {
				//Se ci sono punti di estensione allora ricorsivamente calcolo tutte le classi estese
				while (estensione != null) {
					System.out.println("Tento di scendere di livello");
					estensione = scendi_livello(estensione, types);
					dit_for_Interfaces(estensione, types);
				}
				System.out.println("Ho terminato il while, con livello: " + this.level);
			} else {
				//La classe non possiede punti di estensione osserviamo il suo comportamento rispetto alle interfacce definite
				String[] interfacce = type.getSuperInterfaceNames();
				//Per ogni interfaccia osservo dove si trova corrispondenza con i valori del DIT già ritrovati per le altre interfacce
				for (String s : interfacce) {
					for (DitInheritance dit : inhInter) {
						if (s.equals(dit.getName_interface())) {
							if (this.max_dit < dit.getLevel()) {
								this.max_dit = dit.getLevel();
							}
						}
					}
				}
			}
			System.out.println("Il valore dit per la classe BouncingDrawing è: " + this.max_dit);
		} 
		return ritorno;
	}

	/*
	 * Metodo che dal tipo considera anche il livello del dit in relazione alle interfacce della classe in esame
	 */
	public void dit_for_Interfaces (String estensione, ArrayList<IType> tipi) throws JavaModelException {
		//Ottengo il tipo
		int livello = this.level;
		for (IType t : tipi) {
			if (t.getElementName().equals(estensione)) {
				String[] interfacce = t.getSuperInterfaceNames();
				if (interfacce != null) {
					//Verifico con il dit di tutte le interfacce
					for (DitInheritance dit : inhInter) {
						for (String s : interfacce) {
							if (dit.getName_interface().equals(s)) {
								livello += dit.getLevel();
								System.out.println("Valuto il livello: " + livello);
								if (livello > this.max_dit) {
									System.out.println("Assegno nuovo dit massimo: " + this.max_dit + ", dopo aggiunta livello: " + livello);
									this.max_dit = livello;
									System.out.println("Nuovo maxdit = " + this.max_dit);
									livello = this.level;
								} else {
									livello = this.level;
								}
							}
						}
					}
				}
			}
		}
	}

	/*
	 * Metodo che ricorsivamente permette di scendere di livello il valore del dit associato ad una classe in esame, inoltre
	 * permette anche ad ogni occorrenza di estrapolare le interfacce e verificare step-by-step il livello massimo del DIT
	 */
	public String scendi_livello (String estensione, ArrayList<IType> tipi) throws JavaModelException {
		System.out.println("Punto di estensione");
		this.level = level + 1;
		String ritorno = "";
		//Ricerca del tipo
		boolean ricerca = false;
		for (IType t : tipi) {
			if (t.getElementName().equals(estensione)) {
				ritorno = t.getSuperclassName();
				ricerca = true;
			}
		}
		if (ricerca == false) {
			//Il tipo in estensione non è un tipo del progetto
			ritorno = null;
			this.level--;
		}
		return ritorno;
	}

	/*
	 * 
	 * Metodo che permette di calcolare la metrica NOC, il numero di children vincolanti a un tipo specifico 
	 * passato come parametro
	 * 
	 * NOTA -> metodo commentato con commenti innnestati per evitare eventuali errori
	 */
	/*
	public void calcolaNoc(IType type, ArrayList<IType> types) throws JavaModelException {
		//Inizializzo la variabile di istanza per un nuovo calcolo per il nuovo tipo in esame
		this.noc = 0;
		//Verifico se il tipo in esame è un'interfaccia
		if (type.isInterface()) {
			//A questo punto verifico solo tutte le classi che utilizzano il tipo in esame sotto parola chiave implements
			for (IType t: types) {
				//Estrapolo tutte le estensioni tramite implements del tipo in esame
				String[] all_implements = t.getSuperInterfaceNames();
				//Verifico se tra le stringhe ritornate è presente il tipo in esame, in caso positivo incrementiamo il valore di noc
				for (String inter : all_implements) {
					if (inter.equals(type.getElementName())) {
						//System.out.println("Trovato match con la classe: " + t.getElementName());
						this.noc++;
					}
				}
			}
		}else {
	 */
	/*
	 * Se la classe non è un'interfaccia allora in Java non è possibile utilizzare la parola chiave implements
	 * per una classe astratta o classe normale, quindi dobbiamo controllare solo la parola chiave extends.
	 * Quindi verificare per ogni tipo del progetto se la classe estesa combacia con quella del tipo in esame
	 */
	/*
			for (IType t: types) {
				//Estrapolo estensione dalla parola chiave extends
				String estensione = t.getSuperclassName();
				//Controllo se il nome della classe estesa è uguale a quella del tipo in esame
				if (!(estensione == null) && estensione.equals(type.getElementName())) {
					//System.out.println("Trovato match con la classe: " + t.getElementName());
					//Classe t in esame è un figlio di type, quindi incremento il valore del noc
					this.noc++;
				}
			}
		}
	}
	 */

	/*
	 * Metodo che permette di calcolare il valore della metrica di C&K vincolante al valore di LCOM
	 */
	/*
	public void calcolaLCOM(IType type, IField[] fields, IMethod[] methods) throws JavaModelException {
		double dividendo = methods.length;
		//ArrayList Complessivo per il tipo
		ArrayList<Double> tuttiLCOM = new ArrayList<Double>();
		if (fields.length == 0) {
			tuttiLCOM.add(0.0);
		}

		//Pulizia di tutti i campi
		for (IField field : fields) {
			int valore_lcom_type = 0;
			for (IMethod metodo : methods) {
				//Otteniamo il sorgente del metodo
				String sorgente = metodo.getSource();
				//Verifichiamo se viene utilizzata la variabile in esame
				//In questo caso non dobbiamo controllare anche se il nome della variabile è seguita dal punto
				boolean verifica = sorgente.contains(field.getElementName());
				if (verifica == true) {
					//Match per il metodo incremento il contatore per il field in esame
					valore_lcom_type++;
				}
			}
			//Prima di passare al prossimo campo divido il valore ottenuto per tutti i metodi del progetto
			double riscontro = 1.0 - (valore_lcom_type/dividendo);
			tuttiLCOM.add(riscontro);
		}
		//Dopo aver analizzato tutti i campi per la classe type in esame sommiamo tutti i valori e a quel punto dividiamo per il numero dei campi della classe
		double totale = 0;
		for (Double d : tuttiLCOM) {
			totale+=d;
		}
		//Divido per tutti i tipi, il valore di totale sarà compreso tra 0 e 1
		double tuttifields = tuttiLCOM.size();
		this.LCOM = totale/tuttifields;
		//System.out.println("Per la classe di tipo: " + type.getElementName() + ", il valore di LCOM è: " + this.LCOM);
	}
	 */
}
