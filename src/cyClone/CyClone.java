package cyClone;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class CyClone implements Callable<Void> {

//    @Parameters(index = "0", description = "The file whose checksum to calculate.")
//    private File file;

    @Option(names = {"-S", "--sources"}, split=":", description = "source directories to investigate")
    private String[] src_dir = { "." };

    public static void main(String[] args) throws Exception {
        CommandLine.call(new CyClone(), args);
    }

    @Override
    public Void call() throws Exception {
    	System.out.println("Hello world");
//        byte[] fileContents = Files.readAllBytes(file.toPath());
//        byte[] digest = MessageDigest.getInstance(algorithm).digest(fileContents);
//        System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(digest));
        return null;
    }
}
