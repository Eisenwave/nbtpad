package eisenwave.nbtpad;

import eisenwave.nbt.*;
import eisenwave.nbt.io.*;
import joptsimple.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    
    private static final int EXIT_IOERR = 74;
    
    private static boolean compress = true;
    
    public static void main(String... args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.nonOptions("the file to print or edit").ofType(File.class).isRequired();
        //parser.accepts("a", "the action to perform (print|edit)").withRequiredArg();
        parser.accepts("p", "print the NBT file");
        parser.accepts("e", "edit the NBT file").withRequiredArg().describedAs("editor");
        parser.accepts("r", "read Mojangson file and save as NBT").withRequiredArg().ofType(File.class);
        parser.accepts("u", "uncompressed mode");
        
        OptionSet options = parser.parse(args);
    
        List<?> nonOpt = options.nonOptionArguments();
        if (nonOpt.isEmpty()) {
            parser.printHelpOn(System.out);
            return;
        }
        String path = String.valueOf(nonOpt.get(0));
        
        if (options.has("u"))
            compress = false;
    
        if (options.has("p")) {
            nbtPrint(path);
        }
        
        else if (options.has("e")) {
            //String e = options.hasArgument("e")? String.valueOf(options.valueOf("e")) : System.getenv("EDITOR");
            String e = (String) options.valueOf("e");
            /* if (e == null) {
                System.err.println("Must either provide editor with '-e' option or set 'EDITOR' in environment");
                return;
            } */
            nbtEdit(path, e);
        }
        
        else if (options.has("r")) {
            File r = (File) options.valueOf("r");
            nbtRead(r, path);
        }
        
        else {
            parser.printHelpOn(System.err);
        }
    }
    
    private static void nbtPrint(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("File doesn't exist: "+file.getPath());
            return;
        }
    
        NBTNamedTag rootTag = readNBT(file);
        if (rootTag == null) {
            System.err.println("Reading NBT file failed");
            return;
        }
    
        try {
            new MojangsonSerializer(true).toStream(rootTag, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void nbtEdit(String path, String editor) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("File doesn't exist: "+file.getPath());
            return;
        }
    
        NBTNamedTag sourceNBT = readNBT(file);
        if (sourceNBT == null) {
            System.err.println("Reading source-NBT file failed: " + file);
            return;
        }
    
        File editFile;
        try {
            editFile = File.createTempFile("nbtpad", ".mson", null);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        editFile.deleteOnExit();
        //System.out.println("Created temporary editing file "+editFile);
        
        try {
            new MojangsonSerializer(true).toFile(sourceNBT, editFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Writing temporary Mojangson file failed: " + editFile);
            return;
        }
        
        if (!openEditor(editor, editFile.getPath())) {
            System.err.println("Editing temporary Mojangson file failed: " + editFile);
            return;
        }
    
        NBTNamedTag targetNBT = readMSON(editFile);
        if (targetNBT == null) {
            System.err.println("Reading temporary Mojangson file failed: " + editFile);
            return;
        }
    
        writeNBT(targetNBT, file);
    }
    
    private static void nbtRead(File readFile, String path) {
        if (!readFile.exists()) {
            System.err.println("File doesn't exist: "+readFile.getPath());
            return;
        }
        
        NBTNamedTag rootTag = readMSON(readFile);
        if (rootTag == null) {
            System.err.println("Reading NBT file failed");
            return;
        }
    
        writeNBT(rootTag, new File(path));
    }
    
    private static boolean openEditor(String editor, String path) {
        ProcessBuilder builder = new ProcessBuilder()
            .inheritIO()
            .command(editor, path);
        
        try {
            Process process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    private static NBTNamedTag readNBT(File file) {
        try {
            return new NBTDeserializer(compress).fromFile(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(EXIT_IOERR);
            return null;
        }
    }
    
    private static void writeNBT(NBTNamedTag nbt, File file) {
        try {
            new NBTSerializer(compress).toFile(nbt, file);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(EXIT_IOERR);
        }
    }
    
    private static NBTNamedTag readMSON(File file) {
        try {
            return new MojangsonDeserializer().fromFile(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(EXIT_IOERR);
            return null;
        }
    }
    
}
