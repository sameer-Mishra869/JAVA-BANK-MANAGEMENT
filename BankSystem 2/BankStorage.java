import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Saves and loads the whole Bank object to/from a file (Java serialization). */
public final class BankStorage {
    private BankStorage() { }

    /** ~/BankSystemData/bank_data.ser - the same place no matter where the program is started from. */
    public static File defaultFile() {
        return new File(new File(System.getProperty("user.home"), "BankSystemData"), "bank_data.ser");
    }

    public static void save(Bank bank, File file) throws IOException {
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create folder " + dir);
        }
        // write to a temp file first so a crash mid-save can never corrupt the real data file
        File tmp = new File(file.getPath() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))) {
            out.writeObject(bank);
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /** Returns the saved Bank, or null if there is no (readable) save file. */
    public static Bank load(File file) {
        if (!file.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            Object obj = in.readObject();
            if (!(obj instanceof Bank)) throw new IOException("Unexpected data in save file.");
            Bank bank = (Bank) obj;
            bank.afterLoad();
            return bank;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("Could not read " + file + " (" + e + "). Keeping a backup and starting fresh.");
            File backup = new File(file.getPath() + ".corrupt");
            backup.delete();
            file.renameTo(backup);
            return null;
        }
    }
}
