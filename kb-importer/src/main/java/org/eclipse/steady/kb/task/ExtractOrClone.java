package org.eclipse.steady.kb.task;

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.ArrayList;
import org.apache.logging.log4j.Logger;

import org.eclipse.steady.shared.util.FileUtil;
import org.eclipse.steady.kb.model.Vulnerability;
import org.eclipse.steady.kb.model.Commit;
import org.eclipse.steady.kb.Import;
import org.eclipse.steady.kb.util.Metadata;
import org.eclipse.steady.kb.Manager;

public class ExtractOrClone {

  private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

  private static final String GIT_DIRECTORY = "git-repos";
  private final Manager manager;
  private final Vulnerability vuln;
  private final String vulnId;
  private final String dirPath;
  private final File tarFile;
  private final boolean skipClone;

  public ExtractOrClone(Manager manager, Vulnerability vuln, File dir, boolean skipClone) {
    this.manager = manager;
    this.vuln = vuln;
    this.vulnId = vuln.getVulnId();
    this.dirPath = dir.getPath();
    this.tarFile = getTarFile(dirPath);
    this.skipClone = skipClone;
  }

  public void execute() {

    if (tarFile != null) {
      manager.setVulnStatus(this.vulnId, Manager.VulnStatus.EXTRACTING);
      extract(tarFile, dirPath);
    } else {
      List<Commit> commits = vuln.getCommits();
      if (commits == null || commits.size() == 0) {
        return;
      } else if (this.skipClone) {
        log.info("Skipping clone for vulnerability " + this.vulnId);
        manager.setVulnStatus(this.vulnId, Manager.VulnStatus.SKIP_CLONE);
      } else {
        manager.setVulnStatus(this.vulnId, Manager.VulnStatus.CLONING);
        log.info("Cloning repository for vulnerability " + this.vulnId);
        clone(vuln, dirPath);
      }
    }
    log.info("ExtractOrClone : done (" + dirPath + ")");
  }

  public File getTarFile(String dirPath) {
    if (FileUtil.isAccessibleFile(dirPath + File.separator + Import.SOURCE_TAR)) {
      return new File(dirPath + File.separator + Import.SOURCE_TAR);
    } else return null;
  }

  public void extract(File tarFile, String dirPath) {

    log.info("Extracting vulnerability " + vulnId);
    String extractCommand = "tar -xf " + tarFile.getPath() + " --directory " + dirPath;

    try {

      Process process = Runtime.getRuntime().exec(extractCommand);
      process.waitFor();

      List<Commit> commits = vuln.getCommits();
      for (Commit commit : commits) {
        String commitDirPath = dirPath + File.separator + commit.getCommitId();
        createAndWriteCommitMetadata(commit, null, commitDirPath);
      }

    } catch (IOException | InterruptedException e) {
      String vulnId = dirPath.split(File.separator)[dirPath.split(File.separator).length - 1];
      manager.setVulnStatus(vulnId, Manager.VulnStatus.FAILED_EXTRACT_OR_CLONE);
      manager.addFailure(vuln.getVulnId(), e.toString());
      e.printStackTrace();
    }
  }

  public void clone(Vulnerability vuln, String dirPath) {

    List<Commit> commits = vuln.getCommits();
    for (Commit commit : commits) {
      String repoUrl = commit.getRepoUrl();
      String commitId = commit.getCommitId();
      String commitDirPath = dirPath + File.separator + commitId;
      //System.out.println("commitDirPath : " + commitDirPath);
      File commitDir = new File(commitDirPath);
      commitDir.mkdir();
      String repoDirPath = dirPath + File.separator + 
          GIT_DIRECTORY + File.separator + repoUrl.replace("https://", "").replace("/", "_");
      manager.lockRepo(repoUrl);
      try {
        cloneOnce(repoUrl, repoDirPath);
        createAndWriteCommitMetadata(commit, repoDirPath, commitDirPath);
        writeCommitDiff(commitId, repoDirPath, commitDirPath);
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        manager.setVulnStatus(vuln.getVulnId(), Manager.VulnStatus.FAILED_EXTRACT_OR_CLONE);
        manager.addFailure(vuln.getVulnId(), e.toString());
        break;
      }
      manager.unlockRepo(repoUrl);
    }
  }

  public void createAndWriteCommitMetadata(Commit commit, String repoDirPath, String commitDirPath)
      throws IOException {

    String commitId = commit.getCommitId();
    String commitMetadataPath = commitDirPath + File.separator + Import.METADATA_JSON;
    File commitMetadataFile = new File(commitDirPath);
    HashMap<String, String> commitMetadata = new HashMap<String, String>();
    String timestamp;

    if (repoDirPath == null) {
      Path timestampPath = Paths.get(commitDirPath + File.separator + "timestamp");
      timestamp = new String(Files.readAllBytes(timestampPath)).replace("\n", "");
    } else {
      String gitShowCommand =
          "git -C " + repoDirPath + " show --no-patch --no-notes --pretty='%at' " + commitId;
      Process gitShow = Runtime.getRuntime().exec(gitShowCommand);

      BufferedReader gitShowStdInput =
          new BufferedReader(new InputStreamReader(gitShow.getInputStream()));
      log.info("Executing: " + gitShowCommand);
      try {
        gitShow.waitFor();
      } catch (InterruptedException e) {
        return;
      }
      if ((timestamp = gitShowStdInput.readLine()) == null || timestamp == null) {
        BufferedReader gitShowError =
            new BufferedReader(new InputStreamReader(gitShow.getErrorStream()));
        String repoUrl = commit.getRepoUrl();
        log.error("Failed to get commit timestamp for repository " +repoUrl + " commit id " + commitId);
        String error = gitShowError.readLine();
        log.error("git show: " + error);
        manager.setVulnStatus(vuln.getVulnId(), Manager.VulnStatus.FAILED_EXTRACT_OR_CLONE);
        manager.addFailure(vuln.getVulnId(), "Failed to get commit timestamp for repository " +repoUrl + " commit id " + commitId);
      }
    }

    commitMetadata.put("repository", commit.getRepoUrl());
    commitMetadata.put("branch", commit.getBranch());
    commitMetadata.put("timestamp", timestamp);
    commitMetadata.put("commit_id", commitId);

    Metadata.writeCommitMetadata(commitDirPath, commitMetadata);
  }

  public void cloneOnce(String repoUrl, String repoDirPath)
      throws IOException, InterruptedException {

    if (Files.exists(Paths.get(repoDirPath))) {
      log.info("Folder " + repoDirPath + " exists. Skipping git clone.");
    } else {
      log.info("Cloning repository " + repoUrl);
      String gitCloneCommand = "git clone " + repoUrl + " " + repoDirPath;
      Process gitClone = Runtime.getRuntime().exec(gitCloneCommand);
      gitClone.waitFor();
    }
  }

  public void writeCommitDiff(String commitId, String repoDirPath, String commitDirPath)
      throws IOException, InterruptedException {
    String gitDiffCommand =
        "git -C " + repoDirPath + " diff --name-only " + commitId + "^.." + commitId;
    log.info("Executing: " + gitDiffCommand);
    Process gitDiff = Runtime.getRuntime().exec(gitDiffCommand);
    BufferedReader gitDiffStdInput =
        new BufferedReader(new InputStreamReader(gitDiff.getInputStream()));

    String filename;
    while ((filename = gitDiffStdInput.readLine()) != null) {
      execGitDiffFile(repoDirPath, commitId, filename, true);
      execGitDiffFile(repoDirPath, commitId, filename, false);
    }
  }

  public void execGitDiffFile(String repoDirPath, String commitId, String filename, boolean before)
      throws IOException, InterruptedException {

    String commitDirPath = dirPath + File.separator + commitId;
    String commitStr;
    if (before) { 
      commitStr = commitId + "~1:";
    } else {
      commitStr = commitId + ":";
    }
    String beforeOrAfter = before? "before" : "after";
    // for each file modified in the commit...
    String gitCatCommand =
        "git -C " + repoDirPath + " cat-file -e " + commitStr + filename;
    Process gitCat = Runtime.getRuntime().exec(gitCatCommand);
    log.info("Executing: " + gitCatCommand);
    BufferedReader gitCatErrorInput =
        new BufferedReader(new InputStreamReader(gitCat.getErrorStream()));
    gitCat.waitFor();
    if (gitCat.exitValue() == 0) {
      String filepath = commitDirPath + File.separator + beforeOrAfter + File.separator + filename;
      File file = new File(filepath);
      File dir = file.getParentFile();
      dir.mkdirs();
      
      String diffFileCommand =
          "git -C "
              + repoDirPath
              + " show "
              + commitId
              + "~1:"
              + filename;

      log.info("Executing: " + diffFileCommand);
      Process gitDiffFile = Runtime.getRuntime().exec(diffFileCommand);
    
      writeCmdOutputToFile(gitDiffFile, filepath);
    
      gitDiffFile.waitFor();

    } else {
      log.error("git cat-file didn't work");
      log.error(gitCatErrorInput.readLine());
      // What to do in case it doesn't work?
      manager.setVulnStatus(vulnId, Manager.VulnStatus.FAILED_EXTRACT_OR_CLONE);
      manager.addFailure(vuln.getVulnId(), "git cat-file didn't work");
    }
  }

  public void writeCmdOutputToFile(Process process, String filepath) throws IOException {
    BufferedReader stdInput =
        new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    String lines = "";
    while ((line = stdInput.readLine()) != null) {
      lines += line + "\n";
    }
    Path path = Paths.get(filepath);
    byte[] bytes = lines.getBytes();

    Files.write(path, bytes);
  }

}
