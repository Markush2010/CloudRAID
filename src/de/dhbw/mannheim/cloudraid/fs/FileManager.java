package de.dhbw.mannheim.cloudraid.fs;

import java.io.File;
import java.util.NoSuchElementException;

import de.dhbw.mannheim.cloudraid.jni.RaidAccessInterface;

/**
 * Reads as a thread the {@link FileQueue} and handles the files according to
 * the {@link FileQueueEntry}.
 * 
 * @author Florian Bausch
 * 
 */
public class FileManager extends Thread {

	/**
	 * The temp directory for cloudraid.
	 */
	private final static String TMP = System.getProperty("java.io.tmpdir")
			+ File.separator + "cloudraid" + File.separator;

	/**
	 * The file object of the temp directory.
	 */
	private final static File TMP_FILE = new File(TMP);

	private final static String KEY = "key";
	private final static int KEYLENGTH = KEY.length();

	private int interval = 2000;

	static {
		TMP_FILE.mkdirs();
	}

	/**
	 * Creates a FileManager thread with minimal priority.
	 */
	public FileManager() {
		this.setPriority(MIN_PRIORITY);
	}

	/**
	 * Creates a FileManager thread with minimal priority. Sets the name of the
	 * Thread to "FileManager-" + i and sets the interval of scanning the
	 * {@link FileQueue} to (i+1)*2s.
	 * 
	 * @param i
	 *            The number of the thread.
	 */
	public FileManager(int i) {
		this();
		this.setName("FileManager-" + i);
		this.interval = (i + 1) * 2000;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		boolean wasFull = false;
		long startTime = 0;
		while (!this.isInterrupted()) {
			if (FileQueue.isEmpty()) {
				if (wasFull)
					System.err.println("Time: "
							+ (System.currentTimeMillis() - startTime));
				wasFull = false;
				try {
					sleep(this.interval);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			} else {
				if (!wasFull)
					startTime = System.currentTimeMillis();
				wasFull = true;
				FileQueueEntry fqe = null;
				try {
					fqe = FileQueue.get();
				} catch (NoSuchElementException e) {
					System.err.println("Empty queue!");
					continue;
				}

				if (FileLock.lock(fqe.getFileName(), this)) {
					switch (fqe.getFileAction()) {
					case CREATE:
						System.out.println("Upload new file "
								+ fqe.getFileName());
						splitFile(fqe.getFileName());
						break;

					case DELETE:
						System.out.println("Send delete order for "
								+ fqe.getFileName());
						break;

					case MODIFY:
						System.out.println("Upload updated file "
								+ fqe.getFileName());
						splitFile(fqe.getFileName());
						break;
					default:
						System.err.println("This should not happen.");
						break;
					}
					FileLock.unlock(fqe.getFileName(), this);
				} else {
					System.err.println("File " + fqe.getFileName()
							+ " already locked");
					try {
						sleep(this.interval);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
				}
			}
		}
		System.err.println("FileManager thread stopped");
	}

	/**
	 * Splits the file and merges it again.
	 * 
	 * @param filename
	 *            The file to be merged and split.
	 */
	private void splitFile(String filename) {

		// System.out.println("Start splitting " + filename);
		// Split the file into three RAID5 redundant files.
		if (!new File(filename).exists()) {
			System.err.println("The file " + filename
					+ " is not existing anymore");
			return;
		}
		String hashedFilename = RaidAccessInterface.splitInterface(filename,
				TMP, KEY, KEYLENGTH);
		String name = new File(filename).getName();
		RaidAccessInterface.mergeInterface(TMP, hashedFilename, TMP + name,
				KEY, KEYLENGTH);

		/* Do something fancy. */

		// Delete the split files.
		new File(TMP + hashedFilename + ".0").delete();
		new File(TMP + hashedFilename + ".1").delete();
		new File(TMP + hashedFilename + ".2").delete();
		new File(TMP + hashedFilename + ".m").delete();
		new File(TMP + name).delete();

	}

}
