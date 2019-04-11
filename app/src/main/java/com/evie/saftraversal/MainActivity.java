package com.evie.saftraversal;

import android.Manifest;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity
{

   public class ElapsedTime
   {

      long mTime;

      public ElapsedTime()
      {
         mTime = System.nanoTime();
      }

      public long elapsedTimeInMs()
      {
         return (System.nanoTime() - mTime) / 1000000;
      }
   }

   private static final String TAG = "LOL";
   private static final int REQUEST_DOCUMENT_PROVIDER_TRAVERSAL = 1;
   private static final int REQUEST_DOCUMENT_FILE_TRAVERSAL = 2;
   private static final int REQUEST_FULL_TEST = 3;
   private static final int TEST_RUNS = 10;
   private static boolean OPEN_CLOSE_FILES = false;
   ContentResolver contentResolver;
   ReentrantLock mTestLock = new ReentrantLock();
   private int mFilesTraversed;
   private int mFoldersTraversed;
   private CheckBox mOpenFiles;
   private TextView mReport;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      Toolbar toolbar = findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      mReport = findViewById(R.id.report);
      mOpenFiles = findViewById(R.id.openFiles);

      contentResolver = getContentResolver();

      if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED)
      {
         ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
      }

      findViewById(R.id.test_all).setOnClickListener(v ->
      {
         Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
         startActivityForResult(intent, REQUEST_FULL_TEST);
      });
      findViewById(R.id.test_file_button).setOnClickListener(v -> startTest(() -> traverseUsingFilesAPI()));
      findViewById(R.id.test_document_provider_button).setOnClickListener(v ->
      {
         Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
         startActivityForResult(intent, REQUEST_DOCUMENT_PROVIDER_TRAVERSAL);
      });
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
   {
      super.onActivityResult(requestCode, resultCode, data);

      if(requestCode == REQUEST_DOCUMENT_PROVIDER_TRAVERSAL && resultCode == RESULT_OK)
      {
         startTest(() ->
         {
            try
            {
               traverseTree(null, data.getData());
            }
            catch(RemoteException e)
            {
               e.printStackTrace();
            }
         });
      }
      else if(requestCode == REQUEST_FULL_TEST && resultCode == RESULT_OK)
      {
         fullTest(data.getData());
      }
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   }

   public void closeQuietly(AutoCloseable closeable)
   {
      if(closeable != null)
      {
         try
         {
            closeable.close();
         }
         catch(RuntimeException rethrown)
         {
            throw rethrown;
         }
         catch(Exception ignored)
         {
         }
      }
   }

   private void fullTest(Uri uri)
   {
      runOnUiThread(() ->
      {
         OPEN_CLOSE_FILES = mOpenFiles.isChecked();
         mReport.setText("Running test...");
         new Thread(() ->
         {
            if(mTestLock.tryLock())
            {
               try
               {
                  ElapsedTime et = new ElapsedTime();
                  for(int i = 0; i < TEST_RUNS; i++)
                  {
                     traverseUsingFilesAPI();
                     runOnUiThread(() -> mReport.append("..."));
                  }
                  long averageFilesAPIRunTime = et.elapsedTimeInMs() / TEST_RUNS;
                  runOnUiThread(() -> mReport.append("Files API done"));

                  et = new ElapsedTime();
                  for(int i = 0; i < TEST_RUNS; i++)
                  {
                     try
                     {
                        traverseTree(null, uri);
                     }
                     catch(Throwable e)
                     {
                     }
                     runOnUiThread(() -> mReport.append("..."));
                  }
                  long averageSAFRunTime = et.elapsedTimeInMs() / TEST_RUNS;
                  runOnUiThread(() -> mReport.append("SAF done"));

                  runOnUiThread(() ->
                  {
                     StringBuilder report = new StringBuilder();
                     if(OPEN_CLOSE_FILES)
                     {
                        report.append("With opening files\n");
                     }
                     else
                     {
                        report.append("Without opening files\n");
                     }
                     report.append("Number of folders = " + mFoldersTraversed);
                     report.append("\n");
                     report.append("Number of files = " + mFilesTraversed);
                     report.append("\n");
                     report.append("Files API Time = " + averageFilesAPIRunTime + "ms");
                     report.append("\n");
                     report.append("SAF API Time = " + averageSAFRunTime + "ms");
                     report.append("\n");
                     report.append("SAF is therefore a slowdown of ");
                     String slowdown = String.format("%.1fx", ((double) averageSAFRunTime) / averageFilesAPIRunTime);
                     report.append(slowdown);

                     mReport.setText(report);
                     Log.e(TAG, report.toString());
                  });
               }
               finally
               {
                  mTestLock.unlock();
               }
            }
            else
            {
               Log.e(TAG, "Already running a test!");
            }

         }).start();
      });
   }

   private void startTest(Runnable runnable)
   {
      runOnUiThread(() ->
      {
         OPEN_CLOSE_FILES = mOpenFiles.isChecked();
         mReport.setText("Running test...");
         new Thread(() ->
         {
            if(mTestLock.tryLock())
            {
               try
               {
                  ElapsedTime et = new ElapsedTime();
                  for(int i = 0; i < TEST_RUNS; i++)
                  {
                     runnable.run();
                     runOnUiThread(() -> mReport.append("..."));
                  }
                  long averageRunTimeMS = et.elapsedTimeInMs() / TEST_RUNS;
                  runOnUiThread(() ->
                  {
                     StringBuilder report = new StringBuilder();
                     if(OPEN_CLOSE_FILES)
                     {
                        report.append("With opening files\n");
                     }
                     else
                     {
                        report.append("Without opening files\n");
                     }
                     report.append("Number of folder = " + mFoldersTraversed);
                     report.append("\n");
                     report.append("Number of files = " + mFilesTraversed);
                     report.append("\n");
                     report.append("Average Run Time = " + averageRunTimeMS + "ms");

                     mReport.setText(report);
                     Log.e(TAG, report.toString());
                  });
               }
               finally
               {
                  mTestLock.unlock();
               }
            }
            else
            {
               Log.e(TAG, "Already running a test!");
            }

         }).start();
      });
   }

   //Not using right now, but trust me, it is slow.
   private void traverseDocumentTree(Uri data)
   {
      DocumentFile file = DocumentFile.fromTreeUri(this, data);

      String displayName = file.getName();
      String mimeType = file.getType();
      Log.d(TAG, "root=" + displayName + ", mime=" + mimeType);

      mFoldersTraversed = 1;
      mFilesTraversed = 0;
      Queue<DocumentFile> queue = new ArrayDeque<>();

      queue.add(file);

      while(queue.size() > 0)
      {
         DocumentFile curFile = queue.remove();
         traverseDocumentTree(curFile, queue);
      }
   }

   private void traverseDocumentTree(DocumentFile documentFile, Queue<DocumentFile> queue)
   {
      DocumentFile[] children = documentFile.listFiles();

      for(DocumentFile child : children)
      {
         String displayName = child.getName();
         boolean isDirectory = child.isDirectory();
//            Log.d(TAG, "found child=" + displayName + ", isDir=" + isDirectory + ", parent=" + documentFile
// .getUri());

         if(isDirectory)
         {
            mFoldersTraversed++;
            queue.add(child);
         }
         else
         {
            mFilesTraversed++;
            if(OPEN_CLOSE_FILES)
            {
               Uri bottomChild = child.getUri();

               try
               {
                  InputStream inputStream = getContentResolver().openInputStream(bottomChild);

                  if(inputStream != null)
                  {
                     inputStream.close();
                  }
               }
               catch(Exception e)
               {

               }
            }
         }
      }
   }

   private void traverseFileTree(File curFile, Queue<File> queue)
   {
      File[] files = curFile.listFiles();

      for(File file : files)
      {
         if(file.isDirectory())
         {
            mFoldersTraversed++;
            queue.add(file);
         }
         else
         {
            mFilesTraversed++;
            if(OPEN_CLOSE_FILES)
            {
               try
               {
                  FileInputStream fis = new FileInputStream(file);
                  fis.close();
               }
               catch(FileNotFoundException e)
               {
                  e.printStackTrace();
               }
               catch(IOException e)
               {
                  e.printStackTrace();
               }
            }
         }
      }
   }

   private void traverseTree(ContentProviderClient cpc, Uri uri) throws RemoteException
   {
      String docId = DocumentsContract.getTreeDocumentId(uri);

      Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
          docId);

      boolean owner = false;
      if(cpc == null)
      {
         cpc = contentResolver.acquireContentProviderClient(docUri);
         owner = true;
      }

      if(cpc != null)
      {
         try
         {
            Cursor docCursor = cpc.query(docUri, new String[]{
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE},
                null,
                null,
                null);
            try
            {
               while(docCursor.moveToNext())
               {
//                Log.d(TAG, "root doc =" + docCursor.getString(0) + ", mime=" + docCursor
//                        .getString(1));
               }
            }
            finally
            {
               closeQuietly(docCursor);
            }

            Queue<String> queue = new ArrayDeque<>();

            queue.add(docId);

            mFilesTraversed = 0;
            mFoldersTraversed = 1;
            while(queue.size() > 0)
            {
               String currentDocId = queue.remove();
               traverseTree(uri, currentDocId, queue);
            }
         }
         finally
         {
            if(owner)
            {
               cpc.close();
            }
         }
      }
   }

   private void traverseTree(Uri rootUri, String documentId, Queue<String> queue)
   {
      Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri,
          documentId);

      Cursor cursor = contentResolver.query(childrenUri, new String[]{
          DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
          DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null);
      try
      {
         while(cursor.moveToNext())
         {
            String displayName = cursor.getString(0);
            String mimeType = cursor.getString(1);
//                String documentID = cursor.getString(2);
//                Log.d(TAG, "found child=" + displayName + ", mime=" + mimeType + ", parent=" + documentId);

            if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType))
            {
               mFoldersTraversed++;
               queue.add(cursor.getString(2));
            }
            else
            {
               mFilesTraversed++;
               if(OPEN_CLOSE_FILES)
               {
                  Uri bottomChild = DocumentsContract.buildChildDocumentsUriUsingTree(childrenUri, documentId);

                  try
                  {
                     InputStream inputStream = getContentResolver().openInputStream(bottomChild);

                     if(inputStream != null)
                     {
                        inputStream.close();
                     }
                  }
                  catch(Exception e)
                  {

                  }
               }
            }
         }
      }
      finally
      {
         closeQuietly(cursor);
      }
   }

   private void traverseUsingFilesAPI()
   {
      File root = Environment.getExternalStorageDirectory();

      Queue<File> queue = new ArrayDeque<>();
      queue.add(root);

      mFoldersTraversed = 1;
      mFilesTraversed = 0;
      while(queue.size() > 0)
      {
         File curFile = queue.remove();
         traverseFileTree(curFile, queue);
      }
   }
}
