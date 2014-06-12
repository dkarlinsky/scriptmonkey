import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Computable
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

import javax.swing.SwingUtilities
def result = null
SwingUtilities.invokeAndWait {
    result = application.runWriteAction({ ->

        def module = ModuleManager.getInstance(project).getModules()[0]

        def contentRoot = ModuleRootManager.getInstance(module).getModifiableModel().getContentEntries()[0]
        //contentRoot.getSourceFolders().collect{"${it.rootType} - ${it.file}"}.join("\n")
        contentRoot.addSourceFolder(contentRoot.getFile().findFileByRelativePath("src/main/java"), JavaSourceRootType.SOURCE)
        contentRoot.addSourceFolder(contentRoot.getFile().findFileByRelativePath("src/main/resources"), JavaResourceRootType.RESOURCE)


    } as Computable)
}

result