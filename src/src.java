import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner; // Import the Scanner class to read text files

// Code base on INF600H_TP1 
public class src {
    private static String[] endOfSetence = { ".", "!", "?" };

    private static String[] periodWords = { "Mr.", "Mrs.", "Ms.", "Prof.", "Dr.", "Gen.", "Rep.", "Sen.", "St.", "Sr.",
            "Jr.", "Ph.", "Ph.D.", "M.D.", "B.A.", "M.A.", "D.D.", "D.D.S.", "B.C.", "b.c.", "a.m.", "A.M.", "p.m.",
            "P.M.", "A.D.", "a.d.", "B.C.E.", "C.E.", "i.e.", "etc.", "e.g.", "al." };

    private static ArrayList<String> setences = new ArrayList<>();
    private static Map<Integer, ArrayList<String>> setenceWordDictionary = new HashMap<Integer, ArrayList<String>>();
    public static void main(String[] args) throws FileNotFoundException {
        Scanner myObj = new Scanner(System.in); // Create a Scanner object
        System.out.println("Enter file to analyse ex (eval.txt):");

        String files = myObj.nextLine(); // Read user input
        System.out.println("file is: " + files); // Output user input
        myObj.close();
        File file = new File(files); 
        setences = GetSetences(file);

        Map<Integer, String> setencesDictionary = GetSetencesDictionary();

        setenceWordDictionary =  GetSetenceWordDictionary(setencesDictionary);

        /// Algo principale reste a etre implemente 

    }


    // Set dictionary of setences 
    // setence 1 = "Setence 1"
    // setence 2 = "Setence 2"
    // etc ....
    public static Map<Integer, String> GetSetencesDictionary() {

        Map<Integer, String> setencesDictionary = new HashMap<Integer, String>();

        for (int i = 0; i < setences.size(); i++) {
            setencesDictionary.put(i, setences.get(i));
        }
        return setencesDictionary;
    }

    // Set dictionary of word by setences nb 
    // setence 1 = ["wordx", "wordy", ... "wordn"]
    // setence 2 = ["wordx", "wordy", ... "wordn"]
    // etc ....
    public static Map<Integer, ArrayList<String>> GetSetenceWordDictionary(Map<Integer, String> setencesDictionary){
        Map<Integer, ArrayList<String>> SetenceWordDictionary = new HashMap<Integer, ArrayList<String>>();
        for(Integer setenceNb : setencesDictionary.keySet())
        {
            Scanner s = new Scanner(setencesDictionary.get(setenceNb));
            s.useDelimiter("[\\p{Punct}+\\p{javaWhitespace}+]");
            ArrayList<String> setence = new ArrayList<>();
            while (s.hasNext())
            {
                setence.add(s.next());
            }
            SetenceWordDictionary.put(setenceNb, setence);
        }
        return SetenceWordDictionary;
    }


    //idf(m) = log2(n / |{Pi|m ∈ Pi}|)
    // calculate the idf of a word 
    public static double GetWordIdf(String word)
    {
        int compteur = 0; 
        for (ArrayList<String> setence : setenceWordDictionary.values())
        {
            if(setence.contains(word)){
                compteur += 1;
            }
        }
        int toTransformeToLog2 = setenceWordDictionary.size() / compteur ;  // (n / |{Pi|m ∈ Pi}|)
        return (Math.log(toTransformeToLog2) / Math.log(2)); // * log2 
    }

    //mSim(m, Pd) = {1 si m ∈ Pd, 0 sinon
    // Calculated if a word is present or not 
    public static int Msim(String word, ArrayList<String> setence)
    {
        if(setence.contains(word)){
            return 1;
        }else{
            return 0;
        }
    }

    //hSim(Ps, Pd) = (∑m∈Ps mSim(m, Pd m ) × idf(m)) /  (∑m∈Ps idf(m))
    // calculate the similarity of a setence by another one 
    public static double Hsim(ArrayList<String> setence1, ArrayList<String> setence2)
    {
        double somme1 = 0;
        double somme2 = 0;
        for (String word : setence1)
        {
            double idf = GetWordIdf(word);
            somme1 =+ Msim(word, setence2) * idf; // ∑m∈Ps mSim(m, Pd m ) × idf(m)
            somme2 =+ idf; // ∑m∈Ps idf(m)
        }

        return somme1/somme2; 
    }

    // pSim(P1, P2) = 1/2[hSim(P1, P2) + hSim(P2, P1)]
    // calculate the similarity of two setences  
    public static double Psim(ArrayList<String> setence1, ArrayList<String> setence2)
    {
        return 0.5*(Hsim(setence1,setence2) + Hsim(setence2, setence1));
    }

    //maxSim(P, S) = max Pi ∈ S pSim(P, Pi) 
    // Calculate the maximum similarity between a setence and a list of setence 
    public static double MaxSim(ArrayList<String> setence, ArrayList<ArrayList<String>> setences){
        double max = Double.NEGATIVE_INFINITY;
        for(ArrayList<String> s : setences) // Pi ∈ S
        {
            double psim = Psim(setence, s); // pSim(P, Pi)
            if(psim > max){
                max = psim; // max
            }
        }
        return max;
    }

    // Get all the setence in the specific file in arg 
    public static ArrayList<String> GetSetences(File filename) throws FileNotFoundException {

        ArrayList<String> setences = new ArrayList<String>();
        
            Scanner s = new Scanner(filename, "UTF-8");
            String setence = "";
            while (s.hasNext()) {
                String word = s.next();

                if (Arrays.stream(periodWords).anyMatch(word::equals)) {
                    setence = setence + word + " ";
                } else if (Arrays.stream(endOfSetence).anyMatch((word.substring(word.length() - 1))::equals)) {
                    word = word.substring(0, word.length() - 1);
                    setence = setence + word;
                    setences.add(setence);
                    setence = "";
                } else {
                    setence = setence + word + " ";
                }

            }
            s.close();
        

        return setences;
    }

}