import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

//NUM_STATES - Number of values in FSM
//NUM_ACCEPTING - This is the number of final states.
//INPUT_LENGTH - Test input string length. 2^INPUT_LENGTH strings are being created.
//POPULATION_SIZE - Number of genomes. I think increasing this won't really matter. 
//NUM_POPS - 'k' The isolated subpopulations.
//ESTIMATION_ITERATIONS - The number of iterations that take place. 
public class EEAFSM{
	private static int NUM_STATES = 10, NUM_ACCEPTING = 2, INPUT_LENGTH = 10, 
		POPULATION_SIZE=10, NUM_POPS = 2, ESTIMATION_ITERATIONS = (int)((3.0/8.0)*NUM_POPS*POPULATION_SIZE),
		MUTATION_GENERATIONS = 50, PRINT_EVERY=25, LIMIT = 1001;
	private static boolean SAVE_CHAMPION = false, SAVE_EXAMPLE = false;
	private static String FILE_PREFIX = "Winner";
	private static FSM<Integer> SEED = null;
	private static Map<ExampleGenerator<Integer>,Map<Integer,List<Double>>> output = new HashMap<ExampleGenerator<Integer>,Map<Integer,List<Double>>>();
	private static final List<ExampleGenerator<Integer>> RUNS = new LinkedList<ExampleGenerator<Integer>>();
	private static ExampleGenerator<Integer> DEFAULT_EXAMPLE_GENERATOR = new RandomExample();
	private static final ListSet<Integer> ALPHABET = new ListSet<Integer>(Arrays.asList(0,1));
	private static final FSM<Integer> TARGET = FSM.randomFactory(NUM_STATES, NUM_ACCEPTING, ALPHABET);
	//private static final FSM<Integer> TARGET = FSM.badHandCodedFactory( ALPHABET);
	private static final Map<List<Integer>,Boolean> labelled = new HashMap<List<Integer>,Boolean>();
	private static final List<List<FSM<Integer>>> populations = new ArrayList<List<FSM<Integer>>>(NUM_POPS);
	private static final Random rand = new Random(System.nanoTime());
	private static final List<List<Integer>> ALL_INPUTS = generateAllInputs(INPUT_LENGTH);
	private static final Comparator<FSM<Integer>> fitnessComp = new Comparator<FSM<Integer>>(){
		public int compare(FSM<Integer> o1, FSM<Integer> o2){return o1.fitness()>o2.fitness()?-1:o2.fitness()>o1.fitness()?1:0;}};
	private static final Comparator<Pair<List<Integer>,Double>> sentComp = new Comparator<Pair<List<Integer>,Double>>(){
		public int compare(Pair<List<Integer>,Double> o1, Pair<List<Integer>,Double> o2){return o1.second>o2.second?-1:o2.second>o1.second?1:0; }};
	private static final class RandomExample implements ExampleGenerator<Integer>{
		public static final ExampleGenerator<Integer> only = new RandomExample();
		private RandomExample(){}
		public List<Integer> generateExample(){return randomSentence();}
		public String toString(){return "Random";}}
	private static final class InOrderExample implements ExampleGenerator<Integer>{
		public static final ExampleGenerator<Integer> only = new InOrderExample();
		private InOrderExample(){}
		Iterator<List<Integer>> it = generateAllInputs(INPUT_LENGTH).iterator();
		public List<Integer> generateExample(){
			if(!it.hasNext()) it = generateAllInputs(INPUT_LENGTH).iterator();
			return it.next();}
		public String toString(){return "InOrder";}}
	private static final class ActiveExample implements ExampleGenerator<Integer>{
		public static final ExampleGenerator<Integer> only = new ActiveExample();
		private ActiveExample(){}
		public List<Integer> generateExample(){
			List<Pair<List<Integer>,Double>> sentencePop = new LinkedList<Pair<List<Integer>,Double>>();
			for(int i = 0;i<POPULATION_SIZE*NUM_POPS;++i) sentencePop.add(new Pair<List<Integer>,Double>(randomSentence(),0.0));
			for(int gen = 0; gen<MUTATION_GENERATIONS;++gen){
				evaluateSentences(sentencePop);
				Collections.sort(sentencePop,sentComp);
				for(int i =0;i<ESTIMATION_ITERATIONS;++i){
					int fit = rand.nextInt(sentencePop.size()),unfit = rand.nextInt(sentencePop.size());
					if(fit>unfit){ int temp = fit; fit = unfit; unfit = temp;}
					sentencePop.get(unfit).first = mutateSentence(sentencePop.get(fit).first);
				}
			}
			evaluateSentences(sentencePop);
			double max = -Double.MAX_VALUE;
			List<Integer> argmax = null;
			for(Pair<List<Integer>,Double> p:sentencePop)
				if(p.second>max){max=p.second; argmax = p.first;}
			return argmax;
		}
		public String toString(){return "Active";}}

	private static void handleArgs(String[] args){
		String s;
		for(Iterator<String> i = Arrays.asList(args).iterator();i.hasNext();){
			s = i.next();
			switch(s.charAt(0)){
			case '-':
				switch(s.charAt(1)){
				case 'r': setRuns(i.next().toLowerCase()); break; // Set runs
				case 'g': setGenerations(i.next()); break; // set generations
				case 'p': setPopulationSize(i.next()); break; // set population of models/tests
				case 's': setSize(i.next()); break; // set size of FSM
				case 'S': setSave(); break;// set champion saved
				case 'f': setFileName(i.next()); break;//set filename to which champ will be saved
				case 'e': setSaveExample(); break;// set save examples
				case 'b': setSeed(i.next()); break;//set seed
				case 'l': setLimit(i.next()); break; //set limit
				default: System.err.println("Unrecognized Option: "+s); break;
				}
				break;
			default: break;
			}
		}
	}
	private static void setLimit(String limit){
		try{LIMIT = Integer.parseInt(limit);
		}catch(NumberFormatException e){ System.err.println("Imparseable limit: "+limit);}
	}
	private static void setSeed(String filename){
		SEED = FSM.read(filename);
	}
	private static void setSaveExample(){
		SAVE_EXAMPLE = true;
	}
	private static void setSave(){
		SAVE_CHAMPION = true;
	}
	private static void setFileName(String s){
		setSave();
		FILE_PREFIX = s;
	}
	private static void setRuns(String s){
		for(char c:s.toCharArray())
			switch(c){
			case 'r': RUNS.add(RandomExample.only); break;
			case 'a': RUNS.add(ActiveExample.only); break;
			case 'i': RUNS.add(InOrderExample.only); break;
			default: System.err.println("Unrecognized Type Requested:"+s); break;
			}
	}
	private static void setGenerations(String s){
		try{MUTATION_GENERATIONS = Integer.parseInt(s);
		}catch(NumberFormatException e){ System.err.println("Imparseable number of generations: "+s);}
	}
	private static void setPopulationSize(String s){
		try{POPULATION_SIZE = Integer.parseInt(s);
		}catch(NumberFormatException e){ System.err.println("Imparseable number for population: "+s);}
	}
	private static void setSize(String s){
		try{NUM_STATES = Integer.parseInt(s);
		}catch(NumberFormatException e){ System.err.println("Imparseable size: "+s);}
	}
	public static void main(String[] args){
		handleArgs(args);
		if(RUNS.isEmpty()) RUNS.add(DEFAULT_EXAMPLE_GENERATOR);
		boolean found;
		for(ExampleGenerator<Integer> eg: RUNS){
			if(!output.containsKey(eg)) output.put(eg, new TreeMap<Integer,List<Double>>());
			found = false;
			init();
			for(int i = 0;i<LIMIT;++i){
				if(i%PRINT_EVERY==0){
					if(!output.get(eg).containsKey(i)) output.get(eg).put(i, new LinkedList<Double>());
					
					//System.err.println("Iteration "+i+" of "+eg);
					for(List<FSM<Integer>> pop:populations){
						double accuracy = pop.get(0).accuracy(TARGET,ALL_INPUTS);
						output.get(eg).get(i).add(accuracy);
						//System.err.println("Max in population fitness: "+pop.get(0).fitness()+" "+labelled.size());
						//System.err.println("Max in population accuracy: "+accuracy);
						if(accuracy == 1.0||i==LIMIT-1){
							for(int j = i+PRINT_EVERY;j<LIMIT;j+=PRINT_EVERY){
								if(!output.get(eg).containsKey(j)) output.get(eg).put(j,new LinkedList<Double>());
								output.get(eg).get(j).add(accuracy);
							}
							//System.err.println("Solution found!");
							if(SAVE_CHAMPION) pop.get(0).write(FILE_PREFIX+eg);
							found = true;
						}
					}
					if(SAVE_EXAMPLE) try{
						BufferedWriter bw = new BufferedWriter(new FileWriter(eg+"Generation"+i));
						bw.write(labelled.toString());
						bw.close();
					}catch(IOException e){e.printStackTrace();}
				}
				if(found) break;
				est();
				List<Integer> nextTest = eg.generateExample();
				labelled.put(nextTest,TARGET.offer(nextTest));
			}
			//System.out.println(output);
		}
		for(ExampleGenerator<Integer> eg:output.keySet()){
			System.out.println(eg+" averages");
			for(int i:output.get(eg).keySet()){
				List<Double> l = output.get(eg).get(i);
				System.out.println(i+": "+mean(l));
			}
		}
	}
	private static double mean(Iterable<Double> i){
		double total=0, size=0;
		for(double d:i){
			total+=d; ++size;
		}
		return (size==0)?0:total/size;
	}
	private static void est(){
		if(populations.size()==0){
			for(int i =0;i<NUM_POPS;++i){
				populations.add(new ArrayList<FSM<Integer>>(POPULATION_SIZE));
				for(int j =0;j<POPULATION_SIZE;++j) populations.get(i).add(SEED==null?FSM.randomFactory(NUM_STATES, NUM_ACCEPTING, ALPHABET):SEED);
			}
		}else{
			for(List<FSM<Integer>> pop:populations){
				ListIterator<FSM<Integer>> li = pop.listIterator(2);
				while(li.hasNext()){ li.next(); li.set(FSM.randomFactory(NUM_STATES, NUM_ACCEPTING, ALPHABET));}
			}
		}
		for(List<FSM<Integer>> pop: populations){
			for(int gen = 0;gen<MUTATION_GENERATIONS;++gen){
				for(FSM<Integer> fsm:pop) fsm.evaluate(labelled);
				Collections.sort(pop,fitnessComp);
				for(int i =0;i<ESTIMATION_ITERATIONS;++i){
					int fit = rand.nextInt(pop.size()),unfit = rand.nextInt(pop.size());
					if(fit>unfit){ int temp = fit; fit = unfit; unfit = temp;}
					if(unfit == 0) unfit = pop.size()-1;
					pop.set(unfit, EvoFSM.mutate(pop.get(fit)));//TODO: Change mutation to also mutate accepting states?
				}
			}
			for(FSM<Integer> fsm:pop) fsm.evaluate(labelled);
			Collections.sort(pop,fitnessComp);
		}
	}
//	private static List<Integer> exp(){
//		List<Pair<List<Integer>,Double>> sentencePop = new LinkedList<Pair<List<Integer>,Double>>();
//		for(int i = 0;i<POPULATION_SIZE*NUM_POPS;++i) sentencePop.add(new Pair<List<Integer>,Double>(randomSentence(),0.0));
//		for(int gen = 0; gen<MUTATION_GENERATIONS;++gen){
//			evaluateSentences(sentencePop);
//			Collections.sort(sentencePop,sentComp);
//			for(int i =0;i<ESTIMATION_ITERATIONS;++i){
//				int fit = rand.nextInt(sentencePop.size()),unfit = rand.nextInt(sentencePop.size());
//				if(fit>unfit){ int temp = fit; fit = unfit; unfit = (temp==0)?sentencePop.size()-1:temp;}
//				sentencePop.get(unfit).first = mutateSentence(sentencePop.get(fit).first);
//			}
//		}
//		evaluateSentences(sentencePop);
//		double max = -Double.MAX_VALUE;
//		List<Integer> argmax = null;
//		for(Pair<List<Integer>,Double> p:sentencePop)
//			if(p.second>max){max=p.second; argmax = p.first;}
//		return argmax;
//	}
	
	private static List<Integer> mutateSentence(List<Integer> sent){
		int mutLoc = rand.nextInt(sent.size());
		List<Integer> ret = new ArrayList<Integer>(sent.size());
		for(int i:sent) ret.add(i);
		ret.set(mutLoc, ret.get(mutLoc)==1?0:1);
		return ret;
	}
	private static void evaluateSentences(List<Pair<List<Integer>, Double>> sentencePop){
		for(Pair<List<Integer>,Double> p : sentencePop){
			p.second = 0.0;//fixed bug!
			for(List<FSM<Integer>> pop:populations)
				//for(FSM<Integer> fsm:pop) p.second+=fsm.offer(p.first)?1:0; Only offer top two to Exploration?
				p.second+=(pop.get(0).offer(p.first)?1:0) + (pop.get(1).offer(p.first)?1:0) ;
			p.second = 1.0 - 2.0*Math.abs(.5-p.second/(2*NUM_POPS));
		}
	}
	private static void init(){
		labelled.clear();
		populations.clear();
		List<Integer> firstExample = randomSentence();
		labelled.put(firstExample, TARGET.offer(firstExample));
	}
	private static List<Integer> randomSentence(){
		List<Integer> ret = new ArrayList<Integer>(INPUT_LENGTH);
		for(int i = 0;i<INPUT_LENGTH;++i) ret.add(ALPHABET.choice());
		return ret;
	}
	private static List<List<Integer>> generateAllInputs(int length){
		int num = (int)Math.pow(2, length),temp;
		List<List<Integer>> all = new ArrayList<List<Integer>>(num--);
		LinkedList<Integer> one = null;
		for(;num>-1;--num){
			one = new LinkedList<Integer>();
			temp = num;
			while(one.size()<length){
				if(temp == 0) one.addFirst(0);
				else{
					one.addFirst(temp&1);
					temp>>=1;
				}
			}
			all.add(one);
		}
		return all;
	}
	
}
class Pair<T,E>{T first;E second;public Pair(T f, E s){first = f; second = s;}}
interface ExampleGenerator<T>{ List<T> generateExample();}

