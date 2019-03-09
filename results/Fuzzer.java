import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Random;

/**
 * CAP6135 Programming Assignment 2
 * Fuzzer:
 * Applies fuzzing techniques to a file called 'cross.jpg' before passing to a program called 'jpg2bmp'.
 * @author Cody S Beebe
 */
public class Fuzzer
{
	// Stores the number of times to run the fuzzer.
	private int _NumRuns;
	
	// Randomizer
	private Random _Randomizer;
	
	// Stores a mapping of error output strings to a number of times it has occurred.
	private HashMap<String, Integer> _FailureOutputs;
	
	/**
	 * Main method for entry point and argument parsing
	 * @param args Usage: java Fuzzer NumRuns
	 */
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("Usage: java Fuzzer <NumRuns>");
			return;
		}
		
		int numRuns = Integer.parseInt(args[0]);
		
		Fuzzer fuzzer = new Fuzzer(numRuns);
		fuzzer.Run();
		fuzzer.PrintResults();
	}
	
	/**
	 * Constructor to provide a number of runs to be executed
	 * @param numRuns The number of runs to be executed
	 */
	public Fuzzer(int numRuns)
	{
		_NumRuns = numRuns;
		
		// Using a constant seed for repeatability
		_Randomizer = new Random(104729);
		
		_FailureOutputs = new HashMap<String, Integer>();
	}
	
	/**
	 * Reads in a JPG file called 'cross.jpg'
	 * Applies a naive value-based fuzzing described herein
	 * Applies a randomized fuzzing described herein a number of times proportional to NumRuns
	 */
	public void Run()
	{		
		// Read in the original image
		File original = new File("cross.jpg");
		byte[] originalFileContent;

		try
		{
			originalFileContent = Files.readAllBytes(original.toPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		
		// Value fuzzing
		// For each valid value of a byte, *b*:
		// 1. Restore the original file
		// 2. For each byte in the file contents
		// 2.1 If the byte is == *b*, set it to 0
		// 3. Test the image
		
		for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i)
		{
			byte[] fileContent = originalFileContent.clone();
			
			for (int byteIndex = 0; byteIndex < fileContent.length; ++byteIndex)
			{
				if (fileContent[byteIndex] == i)
				{
					fileContent[byteIndex] = 0;
				}
			}
			
			Test(fileContent);
		}
		
		// Randomized fuzzing
		// For each iteration:
		// 1. Restore the original file
		// 2. Mutate the file using the described Mutation Strategy
		// 3. Test the image
		for (int iteration = 0; iteration < _NumRuns; ++iteration)
		{
			// Restore the original file
			byte[] fileContent = originalFileContent.clone();
			
			// Mutation Strategy:
			// 1. Let *size* be the number of bytes in the JPG
			// 2. Generate a random integer, *numToChange*, on the range [0, *size*)
			// 3. For each of *numToChange*
			// 3.1 Generate a random integer, *i*, on the range [0, *size*)
			// 3.2 Generate a random byte, *value*, on the range [Byte.MIN_VALUE, BYTE_MAX_VALUE] 
			// 3.3 Set the *i*-th byte in the file to *value*
			
			int numToChange = _Randomizer.nextInt(fileContent.length);

			for (int index = 0; index < numToChange; ++index)
			{
				int i = _Randomizer.nextInt(fileContent.length);
				fileContent[i] = (byte) (Byte.MIN_VALUE + _Randomizer.nextInt(1 + Byte.MAX_VALUE - Byte.MIN_VALUE));
			}
			
			// Test the image
			Test(fileContent);
		}
	}
	
	/**
	 * Helper function to test the given JPG bytes as an input to the jpg2bmp executable.
	 * If an error is found while testing, the standard error is captured and the input JPG is saved with the error's name.
	 * The error is then placed on a HashSet that ensures we do not save another copy of that image.
	 * @param jpgBytes The JPG bytes to test
	 */
	private void Test(byte[] jpgBytes)
	{
		try
		{
			// Write out the jpgBytes to a file for testing
			File testInput = new File("input.jpg");
			Files.write(testInput.toPath(), jpgBytes);
			
			// Run test program
			String[] command = { "bash",  "-c", "./jpg2bmp input.jpg temp.bmp &> errorOutput.txt" };
			Process execution = Runtime.getRuntime().exec(command);
			
			// Check the return value for error
			if (execution.waitFor() > 0)
			{
				// Some error occurred. Check the error output.
				File errorOutput = new File("errorOutput.txt");
				String errorString = new String(Files.readAllBytes(errorOutput.toPath())).trim();
								
				// Ignore non-class related bugs
				if (!errorString.isEmpty())
				{
					// Have we seen this error yet?
					if (!_FailureOutputs.containsKey(errorString))
					{
						// New error, add to hash set so we don't process it again
						_FailureOutputs.put(errorString, 1);
						
						// New error
						// Store the input and the received output
						File storedInput = new File(errorString + ".jpg");
						Files.copy(testInput.toPath(), storedInput.toPath(), StandardCopyOption.REPLACE_EXISTING);
						
						System.out.println(errorString);
					}
					else
					{
						// Old error, increase the found count
						_FailureOutputs.put(errorString, _FailureOutputs.get(errorString) + 1);
					}
				}
			}
		}
		catch (Exception e)
		{
			// This should not happen in normal operation.
			e.printStackTrace();
		}
	}
	
	/**
	 * Helper function to print the results to the console.
	 */
	public void PrintResults()
	{
		System.out.println("Results:");
		for (String error : _FailureOutputs.keySet())
		{
			System.out.println("\t" + error + ": Found " + _FailureOutputs.get(error) + " times.");
		}
	}
}
