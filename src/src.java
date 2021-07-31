import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner; // Import the Scanner class to read text files
import java.util.Set;

// Code base on INF600H_TP1 
public class src {
    private static String[] endOfSetence = { ".", "!", "?" };

    private static String[] periodWords = { "Mr.", "Mrs.", "Ms.", "Prof.", "Dr.", "Gen.", "Rep.", "Sen.", "St.", "Sr.",
            "Jr.", "Ph.", "Ph.D.", "M.D.", "B.A.", "M.A.", "D.D.", "D.D.S.", "B.C.", "b.c.", "a.m.", "A.M.", "p.m.",
            "P.M.", "A.D.", "a.d.", "B.C.E.", "C.E.", "i.e.", "etc.", "e.g.", "al." };

    private static Collection<ArrayList<String>> textsToAnalyse = new ArrayList<>();
    private static ArrayList<String> setences = new ArrayList<>();
    private static Map<Integer, ArrayList<String>> setenceWordDictionary = new HashMap<>();
    private static Map<Integer, ArrayList<String>> resumeSetenceDictionary = new HashMap<>();
    private static int maxWord = 125;
    private static double lambda = 0.5;

    public static void main(String[] args) throws Exception {
        Scanner myObj = new Scanner(System.in); // Create a Scanner object
        System.out.println("Enter file to analyse ex (eval.txt):");

        String files = myObj.nextLine(); // Read user input
        System.out.println("file is: " + files); // Output user input
        myObj.close();

        textsToAnalyse = GetTexts(files);

        RunAnalyseAndResume();

    }

    public static void RunAnalyseAndResume() throws Exception {
        for (ArrayList files : textsToAnalyse) {
            maxWord = 125;
            if (files.size() > 0 && files.size() < 2) {
                Resume(files.get(0).toString());
            }else if(files.size() == 2){
                ArrayList<String> resume = Resume(files.get(0).toString());
                AnalyseRed2(resume, files.get(1).toString());
            }else {
                throw new Exception("Cannot Have more than 2 files on the same line in the entries file");
            }
        }
    }

    public static double Intersection(ArrayList<String> bigramR, ArrayList<String> bigramH){
        double p = 0;
        for (String big : bigramR) {
            if(bigramH.contains(big)){
                p++;
            }
        }
        return p;
    }
    public static void AnalyseRed2(ArrayList<String> resume, String fileName) throws FileNotFoundException{
        File file = new File(fileName);

        ArrayList<String> setenceResult = GetSetences(file);
        ArrayList<String> bigramR = GetBigram(resume);
        ArrayList<String> bigramH = GetBigram(setenceResult);

        double intersection = Intersection(bigramR, bigramH);
        double p = intersection/bigramR.size();
        double r = intersection/bigramH.size();

        double f = (2*p*r)/(p+r);
        System.out.println("Le résumé pour le texte nomDuFichier à donné une f1 = " + f);
    }

    public static ArrayList<String> GetBigram(ArrayList<String> setencesToBigram){
        ArrayList<String> bigram = new ArrayList<>();
        for(String s : setencesToBigram){
            String[] words = s.split(" "); 
            for(int i = 1; i < words.length - 1; i++) {
                bigram.add(words[i-1] + " " + words[i]);
            }
        }
        return bigram;
    }

    public static ArrayList<String> Resume(String fileName) throws FileNotFoundException {
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        File file = new File(fileName);
        setences = GetSetences(file);

        Map<Integer, String> setencesDictionary = GetSetencesDictionary();

        setenceWordDictionary = GetSetenceWordDictionary(setencesDictionary);

        GetSetenceForResume();

        ArrayList<String> resume = GetResume(setencesDictionary);

        WriteResume(resume, fileNameWithoutExt);
        return resume;
    }

    public static void WriteResume(ArrayList<String> resume, String files) {

        try {

            File myObj = new File(files + "_r.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }

            FileWriter myWriter = new FileWriter(myObj);

            for (String setence : resume) {
                myWriter.write(setence + "\n");
            }

            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static ArrayList<String> GetResume(Map<Integer, String> setencesDictionary) {
        ArrayList<String> resume = new ArrayList<>();

        Set<Integer> temp = resumeSetenceDictionary.keySet();

        while (!temp.isEmpty()) {
            int min = Integer.MAX_VALUE;
            for (Integer integer : temp) {
                if (integer < min) {
                    min = integer;
                }
            }
            resume.add(min + " : " + setencesDictionary.get(min));
            temp.remove(min);
        }

        return resume;
    }

    public static void GetSetenceForResume() {
        while (maxWord > 0) {
            if (resumeSetenceDictionary.size() == 0) {
                double max = Double.NEGATIVE_INFINITY;
                int index = 0;
                for (int i = 1; i < setenceWordDictionary.size(); i++) {
                    double score = lambda * Psim(setenceWordDictionary.get(i), setenceWordDictionary.get(0));
                    if (score > max) {
                        max = score;
                        index = i;
                    }
                }
                resumeSetenceDictionary.put(index, setenceWordDictionary.get(index));
                maxWord -= setenceWordDictionary.get(index).size();
            } else {
                double max = Double.NEGATIVE_INFINITY;
                int index = 0;
                for (int i = 1; i < setenceWordDictionary.size(); i++) {
                    if (resumeSetenceDictionary.get(i) == null) {
                        double score = lambda * Psim(setenceWordDictionary.get(i), setenceWordDictionary.get(0))
                                - (1 - lambda) * MaxSim(setenceWordDictionary.get(i), resumeSetenceDictionary.values());
                        if (score > max) {
                            max = score;
                            index = i;
                        }
                    }
                }
                resumeSetenceDictionary.put(index, setenceWordDictionary.get(index));
                maxWord -= setenceWordDictionary.get(index).size();
            }
        }
    }

    // Set dictionary of setences
    // setence 1 = "Setence 1"
    // setence 2 = "Setence 2"
    // etc ....
    public static Map<Integer, String> GetSetencesDictionary() {

        Map<Integer, String> setencesDictionary = new HashMap<>();

        for (int i = 0; i < setences.size(); i++) {
            setencesDictionary.put(i, setences.get(i));
        }
        return setencesDictionary;
    }

    // Set dictionary of word by setences nb
    // setence 1 = ["wordx", "wordy", ... "wordn"]
    // setence 2 = ["wordx", "wordy", ... "wordn"]
    // etc ....
    public static Map<Integer, ArrayList<String>> GetSetenceWordDictionary(Map<Integer, String> setencesDictionary) {
        Map<Integer, ArrayList<String>> SetenceWordDictionary = new HashMap<>();
        for (Integer setenceNb : setencesDictionary.keySet()) {
            Scanner s = new Scanner(setencesDictionary.get(setenceNb));
            s.useDelimiter("[\\p{Punct}+\\p{javaWhitespace}+]");
            ArrayList<String> setence = new ArrayList<>();
            while (s.hasNext()) {
                String word = s.next();
                if (word.length() > 0) {
                    setence.add(word);
                }
            }
            SetenceWordDictionary.put(setenceNb, setence);
        }
        return SetenceWordDictionary;
    }

    // idf(m) = log2(n / |{Pi|m ∈ Pi}|)
    // calculate the idf of a word
    public static double GetWordIdf(String word) {
        int compteur = 0;
        for (ArrayList<String> setence : setenceWordDictionary.values()) {
            if (setence.contains(word)) {
                compteur += 1;
            }
        }
        int toTransformeToLog2 = setenceWordDictionary.size() / compteur; // (n / |{Pi|m ∈ Pi}|)
        return (Math.log(toTransformeToLog2) / Math.log(2)); // * log2
    }

    // mSim(m, Pd) = {1 si m ∈ Pd, 0 sinon
    // Calculated if a word is present or not
    public static int Msim(String word, ArrayList<String> setence) {
        if (setence.contains(word)) {
            return 1;
        } else {
            return 0;
        }
    }

    // hSim(Ps, Pd) = (∑m∈Ps mSim(m, Pd m ) × idf(m)) / (∑m∈Ps idf(m))
    // calculate the similarity of a setence by another one
    public static double Hsim(ArrayList<String> setence1, ArrayList<String> setence2) {
        double somme1 = 0;
        double somme2 = 0;
        for (String word : setence1) {
            double idf = GetWordIdf(word);
            somme1 = +Msim(word, setence2) * idf; // ∑m∈Ps mSim(m, Pd m ) × idf(m)
            somme2 = +idf; // ∑m∈Ps idf(m)
        }

        return somme1 / somme2;
    }

    // pSim(P1, P2) = 1/2[hSim(P1, P2) + hSim(P2, P1)]
    // calculate the similarity of two setences
    public static double Psim(ArrayList<String> setence1, ArrayList<String> setence2) {
        return 0.5 * (Hsim(setence1, setence2) + Hsim(setence2, setence1));
    }

    // maxSim(P, S) = max Pi ∈ S pSim(P, Pi)
    // Calculate the maximum similarity between a setence and a list of setence
    public static double MaxSim(ArrayList<String> setence, Collection<ArrayList<String>> setences) {
        double max = Double.NEGATIVE_INFINITY;
        for (ArrayList<String> s : setences) // Pi ∈ S
        {
            double psim = Psim(setence, s); // pSim(P, Pi)
            if (psim > max) {
                max = psim; // max
            }
        }
        return max;
    }

    // Get all the text to analyse from the main file
    public static Collection<ArrayList<String>> GetTexts(String filename) throws FileNotFoundException {
        Collection<ArrayList<String>> texts = new ArrayList<>();
        File fileToAnalyse = new File(filename);

        Scanner s = new Scanner(fileToAnalyse, "UTF-8");
        while (s.hasNextLine()) {
            String line = s.nextLine();
            Scanner y = new Scanner(line);
            ArrayList<String> temporary = new ArrayList<>();
            while (y.hasNext()) {
                String file = y.next();
                temporary.add(file);
            }
            texts.add(temporary);
        }
        return texts;
    }

    // Get all the setence in the specific file in arg
    public static ArrayList<String> GetSetences(File filename) throws FileNotFoundException {

        ArrayList<String> setences = new ArrayList<String>();

        Scanner s = new Scanner(filename, "UTF-8");
        String setence = "";
        while (s.hasNext()) {
            String word = s.next().toUpperCase();

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
