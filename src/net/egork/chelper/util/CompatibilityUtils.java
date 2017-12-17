package net.egork.chelper.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * this utils class just for supporting the old version.
 *
 * @author Scruel Tao
 */
public class CompatibilityUtils {
    @NotNull
    public static PsiFile getPsiFile(@NotNull Project project, @NotNull VirtualFile file) {
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psi = psiManager.findFile(file);
        if (psi != null) return psi;
        FileType fileType = file.getFileType();
        FileViewProvider viewProvider = psiManager.findViewProvider(file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        boolean ignored = !(file instanceof LightVirtualFile) && FileTypeRegistry.getInstance().isFileIgnored(file);
        VirtualFile vDir = file.getParent();
        PsiDirectory psiDir = vDir == null ? null : PsiManager.getInstance(project).findDirectory(vDir);
        FileIndexFacade indexFacade = FileIndexFacade.getInstance(project);
        StringBuilder sb = new StringBuilder();
        sb.append("valid=").append(file.isValid()).
            append(" isDirectory=").append(file.isDirectory()).
            append(" hasDocument=").append(document != null).
            append(" length=").append(file.getLength());
        sb.append("\nproject=").append(project.getName()).
            append(" default=").append(project.isDefault()).
            append(" open=").append(project.isOpen());
        sb.append("\nfileType=").append(fileType.getName()).append("/").append(fileType.getClass().getName());
        sb.append("\nisIgnored=").append(ignored);
        sb.append(" underIgnored=").append(indexFacade.isUnderIgnored(file));
        sb.append(" inLibrary=").append(indexFacade.isInLibrarySource(file) || indexFacade.isInLibraryClasses(file));
        sb.append(" parentDir=").append(vDir == null ? "no-vfs" : vDir.isDirectory() ? "has-vfs-dir" : "has-vfs-file").
            append("/").append(psiDir == null ? "no-psi" : "has-psi");
        sb.append("\nviewProvider=").append(viewProvider == null ? "null" : viewProvider.getClass().getName());
        if (viewProvider != null) {
            List<PsiFile> files = viewProvider.getAllFiles();
            sb.append(" language=").append(viewProvider.getBaseLanguage().getID());
            sb.append(" physical=").append(viewProvider.isPhysical());
            sb.append(" rootCount=").append(files.size());
            for (PsiFile o : files) {
                sb.append("\n  root=").append(o.getLanguage().getID()).append("/").append(o.getClass().getName());
            }
        }
        throw new AssertionError();
    }
}
