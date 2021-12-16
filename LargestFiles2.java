/*
  Largest Files #2 - Find Largest (Visible) Files in Folder, Subfolders
  Written by: Keith Fenske, http://kwfenske.github.io/
  Thursday, 10 September 2020
  Java class name: LargestFiles2
  Copyright (c) 2020 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 console application to find the biggest files in a list of
  file or folder names given on the command line.  For example, if you have an
  NTFS disk drive mounted as F:\ on Microsoft Windows and want to know if all
  files are small enough to fit on a USB thumb drive (FAT32 format, maximum of
  4 GB per file), then use the following command:

      java  LargestFiles2  F:\

  Running this program on a Windows system image might produce the following
  output:

      Found 2 largest files from 85,732 files in 13,827 folders.
      2,146,435,072 bytes for F:\pagefile.sys
      2,616,549,376 bytes for F:\hiberfil.sys

  You may change the number of files reported.  Options go before file names on
  the command line:

      -n# = number of large files to report; default is -n5
      -s0 = do only given files or folders, no subfolders
      -s1 = -s = process files, folders, and subfolders (default)

  See the "-?" option for a help summary:

      java  LargestFiles2  -?

  There is no graphical interface (GUI) for this program; it must be run from a
  command prompt, command shell, or terminal window.  Run time will depend upon
  how many files and folders are searched.  Most operating systems protect some
  folders, and this program treats those folders as empty.  The code is a
  template for simple applications that process files, folders, and subfolders.

  Apache License or GNU General Public License
  --------------------------------------------
  LargestFiles2 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.
*/

import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors

public class LargestFiles2
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2020 by Keith Fenske.  Apache License or GNU GPL.";
  static final int DEFAULT_NUMBER = 5; // default large files to report
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final int MAX_NUMBER = 999; // maximum large files to report
  static final int MIN_NUMBER = 1; // minimum large files to report
  static final String PROGRAM_TITLE =
    "Find Largest Files in Folder, Subfolders - by: Keith Fenske";

  /* class variables */

  static TreeSet bigList;         // list of largest file sizes
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static long ignoreSize;         // minimum size for inclusion in short list
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static boolean recurseFlag;     // true if we search folders and subfolders
  static long totalFiles;         // total number of files found
  static long totalFolders;       // total number of folders or subfolders
  static int userNumber;          // number of large files to report

/*
  main() method

  We run as a console application.  There is no graphical interface.
*/
  public static void main(String[] args)
  {
    LargestFiles2Data bigEntry;   // one entry (name, size) from <bigList>
    Iterator bigIterate;          // for iterating over elements in <bigList>
    int i;                        // index variable
    String word;                  // one parameter from command line

    /* Initialize global and local variables. */

    bigList = new TreeSet();      // empty list of largest file sizes
    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups
    ignoreSize = -1;              // minimum size for inclusion in short list
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    recurseFlag = true;           // by default, search folders and subfolders
    totalFiles = totalFolders = 0; // no files found yet
    userNumber = DEFAULT_NUMBER;  // default number of large files to report

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.startsWith("-n") || (mswinFlag && word.startsWith("/n")))
      {
        /* This option is followed by the number of large files to report. */

        try { userNumber = Integer.parseInt(word.substring(2)); } // unsigned
        catch (NumberFormatException nfe) { userNumber = -1; } // illegal value
        if ((userNumber < MIN_NUMBER) || (userNumber > MAX_NUMBER))
        {
          System.err.println("Number of large files to report must be from "
            + MIN_NUMBER + " to " + MAX_NUMBER + ": " + args[i]);
                                  // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-s") || (mswinFlag && word.equals("/s"))
        || word.equals("-s1") || (mswinFlag && word.equals("/s1")))
      {
        recurseFlag = true;       // start doing subfolders
      }
      else if (word.equals("-s0") || (mswinFlag && word.equals("/s0")))
        recurseFlag = false;      // stop doing subfolders

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume this is a file or
        folder name. */

        processFileOrFolder(new File(args[i]));
      }
    }

    /* Report all files in our list of largest sizes, in ascending order by
    size, then by name (if two files have the same size). */

    System.out.println();         // blank line
    System.out.println("Found " + bigList.size() + " largest files from "
      + formatComma.format(totalFiles) + " files in "
      + formatComma.format(totalFolders) + " folders.");
    if ((totalFiles > 0) || (totalFolders > 0)) // only if we found something
    {
      bigIterate = bigList.iterator(); // iterator for file names, sizes
      while (bigIterate.hasNext()) // any more names and sizes?
      {
        bigEntry = (LargestFiles2Data) bigIterate.next(); // get name, size
        System.out.println("  " + formatComma.format(bigEntry.size)
          + " bytes for " + bigEntry.name);
      }
      System.exit(EXIT_SUCCESS);
    }
    else                          // no files or folders given by user
    {
      showHelp();                 // show help summary
      System.exit(EXIT_UNKNOWN);  // exit application after printing help
    }

  } // end of main() method


/*
  processFileOrFolder() method

  The caller gives us a Java File object that may be a file, a folder, or just
  random garbage.  Search all files.  Get folder contents and process each file
  found, doing subfolders only if the <recurseFlag> is true.
*/
  static void processFileOrFolder(File givenFile)
  {
    LargestFiles2Data bigEntry;   // one entry (name, size) from <bigList>
    File[] contents;              // contents if <givenFile> is a folder
    String filePath;              // full directory path name for <givenFile>
    long fileSize;                // size of file in bytes
    int i;                        // index variable
    File next;                    // next File object from <contents>

    if (givenFile.isDirectory())  // if this is a folder
    {
      totalFolders ++;            // one more folder found
      System.err.println("Scanning folder: " + givenFile.getPath());
      contents = givenFile.listFiles(); // unsorted, no filter
      if (contents == null)       // happens for protected system folders
      {
        System.err.println("Protected folder: " + givenFile.getPath());
        contents = new File[0];   // replace with an empty array
      }
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        next = contents[i];       // get next File object from <contents>
        if (next.isDirectory())   // is this a subfolder (in the folder)?
        {
          if (recurseFlag)        // should we look at subfolders?
            processFileOrFolder(next); // yes, search this subfolder
          else
            System.err.println("Ignoring subfolder: " + next.getPath());
        }
        else if (next.isFile())   // is this a file (in the folder)?
        {
          processFileOrFolder(next); // call ourself to process this file
        }
        else
        {
          /* File or folder does not exist.  Ignore without comment. */
        }
      }
    }
    else if (givenFile.isFile())  // if this is a file
    {
      totalFiles ++;              // one more file found
      fileSize = givenFile.length(); // size of file in bytes
      if (fileSize >= ignoreSize) // avoid work that we'll never use
      {
        try { filePath = givenFile.getCanonicalPath(); }
                                  // get full directory path name, if possible
        catch (IOException ioe) { filePath = givenFile.getPath(); }
                                  // or accept abstract path name otherwise
        bigList.add(new LargestFiles2Data(filePath, fileSize));
      }
      while (bigList.size() > userNumber) // keep list as short as possible
      {
        /* Our list is full or slightly more.  Remove the first entry, that is,
        the "lowest" or "smallest" in the sorting order.  This also becomes the
        next minimum size to consider for new files.  Question: which is better
        here, an <if> statement or a <while> statement?  Why? */

        bigEntry = (LargestFiles2Data) bigList.first(); // first or smallest
        ignoreSize = bigEntry.size; // next minimum size for new files
        bigList.remove(bigEntry); // remove smallest from short list
      }
    }
    else
      System.err.println("Not a file or folder: " + givenFile.getPath());

  } // end of processFileOrFolder() method


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  LargestFiles2  [options]  fileOrFolderNames");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -n# = number of large files to report; default is -n" + DEFAULT_NUMBER);
    System.err.println("  -s0 = do only given files or folders, no subfolders");
    System.err.println("  -s1 = -s = process files, folders, and subfolders (default)");
    System.err.println();
    System.err.println("Output may be redirected with the \">\" operator.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method

} // end of LargestFiles2 class

// ------------------------------------------------------------------------- //

/*
  LargestFiles2Data class

  A data object to hold the size (in bytes) and path name for one file.  Use
  the Comparable interface so that a short list of the largest files found can
  be kept sorted in a TreeSet.
*/

class LargestFiles2Data implements Comparable
{
  /* class variables */

  String name;                    // full path name for file
  long size;                      // size of file in bytes

  /* constructor (two arguments) */

  public LargestFiles2Data(String givenName, long givenSize)
  {
    this.name = givenName;        // save caller's file path name
    this.size = givenSize;        // save caller's size in bytes
  }

  /* Make objects of this type comparable, so that we can sort in a list. */

  public int compareTo(Object otherObject)
  {
    LargestFiles2Data other = (LargestFiles2Data) otherObject;

    if (this.size < other.size)   // sizes are more important than names
      return(-1);                 // this object is "lower" or "smaller"
    else if (this.size > other.size)
      return(1);                  // this object is "higher" or "bigger"
    else                          // same size, character order for names
      return(this.name.compareTo(other.name));
  }

} // end of LargestFiles2Data class

/* Copyright (c) 2020 by Keith Fenske.  Apache License or GNU GPL. */
