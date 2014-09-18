import com.github.alexarchambault.sbt_notebook.NotebookPlugin._

scalaVersion in ThisBuild := "2.10.4"

notebookHost := "pc-ubuntu.local"

notebookPort := 47123

notebookSecure := false

// fork in Notebook := false
