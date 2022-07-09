package io.netty.handler.codec.http.multipart;








final class FileUploadUtil
{
  private FileUploadUtil() {}
  







  static int hashCode(FileUpload upload)
  {
    return upload.getName().hashCode();
  }
  
  static boolean equals(FileUpload upload1, FileUpload upload2) {
    return upload1.getName().equalsIgnoreCase(upload2.getName());
  }
  
  static int compareTo(FileUpload upload1, FileUpload upload2) {
    return upload1.getName().compareToIgnoreCase(upload2.getName());
  }
}
