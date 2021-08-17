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

    private static String[] periodWords = { "MR.", "MRS.", "MS.", "PROF.", "DR.", "GEN.", "REP.", "SEN.", "ST.", "SR.",
            "JR.", "PH.", "PH.D.", "M.D.", "B.A.", "M.A.", "D.D.", "D.D.S.", "B.C.", "A.M.", "P.M.", "A.D.", "B.C.E."
            ,"C.E.", "I.E.", "ETC.", "E.G.", "AL.", "FIG.", "P.", "COMM.", "PERS.", "C."};

    // private static Collection<ArrayList<String>> textsToAnalyse = new ArrayList<>();
    // private static ArrayList<String> setences = new ArrayList<>();
    // private static Map<Integer, ArrayList<String>> setenceWordDictionary = new HashMap<>();
    // private static Map<Integer, ArrayList<String>> resumeSetenceDictionary = new HashMap<>();
    private static int maxWord = 125;
    private static double lambda = 0.5;

    public static void main(String[] args) throws Exception {
        Scanner myObj = new Scanner(System.in); // Create a Scanner object
        System.out.println("Enter file to analyse ex (FichierEntre.txt):");

        String files = myObj.nextLine(); // Read user input
        System.out.println("file is: " + files); // Output user input
        myObj.close();

        Collection<ArrayList<String>> textsToAnalyse = GetTexts(files);

        RunAnalyseAndResume(textsToAnalyse);

    }

    
    // Get bigram for a array of setences
    public static ArrayList<String> GetBigram(ArrayList<String> setencesToBigram) {
        ArrayList<String> bigram = new ArrayList<>();
        for (String s : setencesToBigram) {
            String[] words = s.split(" ");
            for (int i = 1; i < words.length - 1; i++) {
                bigram.add(words[i - 1] + " " + words[i]);
            }
        }
        return bigram;
    }

    
    // Because we want a better summary of the text the we replace them in the oder
    // that we have found them in the original text
    // With the dictionary we have place them with nb of setence in the texte :
    // setence associate with
    public static ArrayList<String> GetResume(Map<Integer, String> setencesDictionary, Map<Integer, ArrayList<String>> resumeSetenceDictionary) {
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

    // 𝜆 × pSim(Pi,Q) - (1-𝜆) × maxSim(Pi, SR)
    // for each setence not already in sr we need to do it
    // if sr is empty the part after the minus will not be done
    // all the setence for the remuse are stock in resumeSetenceDictionary
    public static Map<Integer, ArrayList<String>> GetSetenceForResume(Map<Integer, ArrayList<String>> setenceWordDictionary) {
        Map<Integer, ArrayList<String>> resume = new HashMap<>();
        while (maxWord > 0) {
            if (resume.size() == 0) {
                double max = Double.NEGATIVE_INFINITY;
                int index = 0;
                for (int i = 1; i < setenceWordDictionary.size(); i++) {
                    double score = lambda * Psim(setenceWordDictionary.get(i), setenceWordDictionary.get(0), setenceWordDictionary);
                    if (score > max) {
                        max = score;
                        index = i;
                    }
                }
                resume.put(index, setenceWordDictionary.get(index));
                maxWord -= setenceWordDictionary.get(index).size();
            } else {
                double max = Double.NEGATIVE_INFINITY;
                int index = 0;
                for (int i = 1; i < setenceWordDictionary.size(); i++) {
                    if (resume.get(i) == null) {
                        double score = lambda * Psim(setenceWordDictionary.get(i), setenceWordDictionary.get(0), setenceWordDictionary)
                                - (1 - lambda) * MaxSim(setenceWordDictionary.get(i), resume.values(), setenceWordDictionary);
                        if (score > max) {
                            max = score;
                            index = i;
                        }
                    }
                }
                resume.put(index, setenceWordDictionary.get(index));
                maxWord -= setenceWordDictionary.get(index).size();
            }
        }
        return resume;
    }

    // Create a summary of the file pass
    public static ArrayList<String> Resume(String fileName) throws FileNotFoundException {
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        File file = new File(fileName);
        
        ArrayList<String> setences = GetSetences(file);

        Map<Integer, String> setencesDictionary = GetSetencesDictionary(setences);

        Map<Integer, ArrayList<String>> setenceWordDictionary = GetSetenceWordDictionary(setencesDictionary);

        Map<Integer, ArrayList<String>> resumeSetenceDictionary = GetSetenceForResume(setenceWordDictionary);

        ArrayList<String> resume = GetResume(setencesDictionary, resumeSetenceDictionary);

        WriteResume(resume, fileNameWithoutExt);
        return resume;
    }

    // Analyse if we have a red2 metric to calculate or not
    // If we have juste a file we create the resume
    // if we have a file to resume and a result, we create the resume and analyse
    // the red2 metric
    public static void RunAnalyseAndResume(Collection<ArrayList<String>> textsToAnalyse) throws Exception {
        for (ArrayList<String> files : textsToAnalyse) {
            maxWord = 125;

            if (files.size() > 0 && files.size() < 2) {
                Resume(files.get(0));
            } else if (files.size() == 2) {
                ArrayList<String> resume = Resume(files.get(0));
                double f = AnalyseRed2(resume, files.get(1));
                System.out.println("Le résumé pour le texte " + files.get(0) + " à donné une f1 = " + f +" et un lambda de :" + lambda);
            } else {
                throw new Exception("Cannot Have more than 2 files on the same line in the entries file");
            }
        }
    }

    // #region Operation Math

    // p = |BR ∩ BH| / |BR|
    // r = |BR ∩ BH| / |BH|
    // f1 = (2pr) / (p + r)
    // Here we analyse the metric Red2 by getting bigram of both summary, program
    // and humain
    public static double AnalyseRed2(ArrayList<String> resume, String fileName) throws FileNotFoundException {
        File file = new File(fileName);

        ArrayList<String> setenceResult = GetSetences(file);
        ArrayList<String> bigramR = GetBigram(resume);
        ArrayList<String> bigramH = GetBigram(setenceResult);

        double intersection = Intersection(bigramR, bigramH);
        double p = intersection / bigramR.size();
        double r = intersection / bigramH.size();

        return (2 * p * r) / (p + r);
    }

    // idf(m) = log2(n / |{Pi|m ∈ Pi}|)
    // calculate the idf of a word
    public static double GetWordIdf(String word, Map<Integer, ArrayList<String>> setenceWordDictionary) {
        int compteur = 0;
        for (ArrayList<String> setence : setenceWordDictionary.values()) {
            if (setence.contains(word)) {
                compteur += 1;
            }
        }
        int toTransformeToLog2 = setenceWordDictionary.size() / compteur; // (n / |{Pi|m ∈ Pi}|)
        return (Math.log(toTransformeToLog2) / Math.log(2)); // * log2
    }

    // hSim(Ps, Pd) = (∑m∈Ps mSim(m, Pd m ) × idf(m)) / (∑m∈Ps idf(m))
    // calculate the similarity of a setence by another one
    public static double Hsim(ArrayList<String> setence1, ArrayList<String> setence2, Map<Integer, ArrayList<String>> setenceWordDictionary) {
        double somme1 = 0;
        double somme2 = 0;
        for (String word : setence1) {
            double idf = GetWordIdf(word, setenceWordDictionary);
            somme1 =+ Msim(word, setence2) * idf; // ∑m∈Ps mSim(m, Pd m ) × idf(m)
            somme2 =+ idf; // ∑m∈Ps idf(m)
        }

        return somme1 / somme2;
    }

    // |BR ∩ BH|
    // Calcul when a bigram is in the other
    public static double Intersection(ArrayList<String> bigramR, ArrayList<String> bigramH) {
        double p = 0;
        for (String big : bigramR) {
            if (bigramH.contains(big)) {
                p++;
            }
        }
        return p;
    }

    // maxSim(P, S) = max Pi ∈ S pSim(P, Pi)
    // Calculate the maximum similarity between a setence and a list of setence
    public static double MaxSim(ArrayList<String> setence, Collection<ArrayList<String>> setences, Map<Integer, ArrayList<String>> setenceWordDictionary) {
        double max = Double.NEGATIVE_INFINITY;
        for (ArrayList<String> s : setences) // Pi ∈ S
        {
            double psim = Psim(setence, s, setenceWordDictionary); // pSim(P, Pi)
            if (psim > max) {
                max = psim; // max
            }
        }
        return max;
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

    // pSim(P1, P2) = 1/2[hSim(P1, P2) + hSim(P2, P1)]
    // calculate the similarity of two setences
    public static double Psim(ArrayList<String> setence1, ArrayList<String> setence2, Map<Integer, ArrayList<String>> setenceWordDictionary) {
        return 0.5 * (Hsim(setence1, setence2, setenceWordDictionary) + Hsim(setence2, setence1, setenceWordDictionary));
    }

    // #endregion

    // #region Operation on file

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

    // Set dictionary of setences
    // setence 1 = "Setence 1"
    // setence 2 = "Setence 2"
    // etc ....
    public static Map<Integer, String> GetSetencesDictionary(ArrayList<String> setences) {

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

    // Write the resume in a {fileName}_r.txt
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

    // #endregion
}
