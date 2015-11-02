package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.action.model.SymfonySymbolSearchModel;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemEx;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class SymfonySymbolSearchAction extends GotoActionBase {

    @Override
    protected void gotoActionPerformed(AnActionEvent paramAnActionEvent) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");
        Project localProject = paramAnActionEvent.getData(CommonDataKeys.PROJECT);
        if (localProject != null) {
            SymfonySymbolSearchModel searchModel = new SymfonySymbolSearchModel(localProject, new ChooseByNameContributor[] { new Symfony2NavigationContributor(localProject) });
            showNavigationPopup(paramAnActionEvent, searchModel, new MyGotoCallback(), null, true);
        }
    }

    @Override
    public void update(AnActionEvent event) {
        super.update(event);

        Project project = event.getData(CommonDataKeys.PROJECT);

        boolean enabled = Symfony2ProjectComponent.isEnabled(project);

        event.getPresentation().setVisible(enabled);
        event.getPresentation().setEnabled(enabled);

    }

    private static class Symfony2NavigationContributor implements ChooseByNameContributorEx, DumbAware {

        final private Project project;
        private ContainerCollectionResolver.ServiceCollector serviceCollector;
        private Map<String, VirtualFile> templateMap;
        private Map<String, Route> routes;
        private Set<String> twigMacroSet;
        private Map<String, LookupElement> lookupElements;

        public Symfony2NavigationContributor(Project project) {
            this.project = project;
        }

        private ContainerCollectionResolver.ServiceCollector getServiceCollector() {
            if(this.serviceCollector == null) {
                this.serviceCollector = new ContainerCollectionResolver.ServiceCollector(this.project);

                this.serviceCollector.addCollectorSource(ContainerCollectionResolver.Source.COMPILER);
                this.serviceCollector.addCollectorSource(ContainerCollectionResolver.Source.INDEX);
            }

            return this.serviceCollector;
        }

        private Map<String, VirtualFile> getTemplateMap() {

            if(this.templateMap == null) {
                this.templateMap = TwigHelper.getTemplateFilesByName(this.project, true, true);
            }

            return this.templateMap;
        }

        private Map<String, Route> getRoutes() {

            if(this.routes == null) {
                this.routes = RouteHelper.getAllRoutes(project);
            }

            return this.routes;
        }

        private Set<String> getTwigMacroSet() {

            if(this.twigMacroSet == null) {
                this.twigMacroSet = TwigHelper.getTwigMacroSet(this.project);
            }

            return this.twigMacroSet;
        }

        private Map<String, LookupElement> getModelLookupElements() {

            if(this.lookupElements == null) {
                List<LookupElement> modelLookupElements = EntityReference.getModelLookupElements(this.project);

                this.lookupElements = new HashMap<String, LookupElement>();
                for(LookupElement lookupElement: modelLookupElements) {
                    this.lookupElements.put(lookupElement.getLookupString(), lookupElement);
                }

            }

            return this.lookupElements;
        }

        @Override
        public void processNames(@NotNull Processor<String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {


            for(String name: getServiceCollector().getServices().keySet()) {
                processor.process(name);
            }

            for(Map.Entry<String, VirtualFile> entry: getTemplateMap().entrySet()) {
                processor.process(entry.getKey());
            }

            for(String name: getRoutes().keySet()) {
                processor.process(name);
            }

            for(String name: getTwigMacroSet()) {
                processor.process(name);
            }

            for(String name: getModelLookupElements().keySet()) {
                processor.process(name);
            }

            for(SymfonyCommand command: SymfonyCommandUtil.getCommands(project)) {
                processor.process(command.getName());
            }

            // Twig Extensions
            TwigExtensionParser twigExtensionParser = new TwigExtensionParser(project);
            for (Map<String, TwigExtension> extensionMap : Arrays.asList(twigExtensionParser.getFilters(), twigExtensionParser.getFunctions())) {
                for(String twigFilter: extensionMap.keySet()) {
                    processor.process(twigFilter);
                }
            }
        }

        @Override
        public void processElementsWithName(@NotNull String name, @NotNull Processor<NavigationItem> processor, @NotNull FindSymbolParameters parameters) {

            for(ContainerService containerService: getServiceCollector().collect()) {
                if(containerService.getName().equals(name)) {

                    String serviceClass = getServiceCollector().resolve(name);
                    if (serviceClass != null) {
                        PhpClass phpClass = PhpElementsUtil.getClassInterface(this.project, serviceClass);
                        if(phpClass != null) {
                            processor.process(new NavigationItemEx(phpClass, containerService.getName(), containerService.isWeak() ? Symfony2Icons.SERVICE_PRIVATE_OPACITY : Symfony2Icons.SERVICE, "Service"));
                        }
                    }

                }
            }

            if(getTemplateMap().containsKey(name)) {
                VirtualFile virtualFile = getTemplateMap().get(name);
                PsiFile psiFile = PsiManager.getInstance(this.project).findFile(virtualFile);
                if(psiFile != null) {
                    processor.process(new NavigationItemEx(psiFile, name, psiFile.getFileType().getIcon(), "Template"));
                }
            }

            if(getRoutes().containsKey(name)) {
                String controllerName = getRoutes().get(name).getController();
                if(controllerName != null) {
                    for(PsiElement psiElement: RouteHelper.getMethodsOnControllerShortcut(this.project, controllerName)) {
                        processor.process(new NavigationItemEx(psiElement, name, Symfony2Icons.ROUTE, "Route"));
                    }
                }
            }

            if(getTwigMacroSet().contains(name)) {
                for(PsiElement macroTarget: TwigHelper.getTwigMacroTargets(project, name)) {
                    processor.process(new NavigationItemEx(macroTarget, name, TwigIcons.TwigFileIcon, "Macro"));
                }
            }

            if(getModelLookupElements().containsKey(name)) {
                PsiElement[] psiElements = EntityHelper.getModelPsiTargets(this.project, name);

                getModelLookupElements().get(name).getLookupString();
                for(PsiElement target: psiElements) {
                    processor.process(new NavigationItemEx(target, name, target.getIcon(0), "Entity"));
                }
            }

            for (SymfonyCommand symfonyCommand : SymfonyCommandUtil.getCommands(project)) {
                if(symfonyCommand.getName().equals(name)) {
                    processor.process(new NavigationItemEx(symfonyCommand.getPsiElement(), name, Symfony2Icons.SYMFONY, "Command"));
                }
            }

            // Twig Extensions
            TwigExtensionParser twigExtensionParser = new TwigExtensionParser(project);
            for (Map<String, TwigExtension> extensionMap : Arrays.asList(twigExtensionParser.getFilters(), twigExtensionParser.getFunctions())) {
                for(Map.Entry<String, TwigExtension> twigFunc: extensionMap.entrySet()) {
                    if(twigFunc.getKey().equals(name)) {
                        TwigExtension twigExtension = twigFunc.getValue();
                        PsiElement extensionTarget = TwigExtensionParser.getExtensionTarget(project, twigExtension);
                        if(extensionTarget != null) {
                            processor.process(new NavigationItemEx(extensionTarget, name, TwigExtensionParser.getIcon(twigExtension.getTwigExtensionType()), twigExtension.getTwigExtensionType().toString()));
                        }
                    }
                }
            }
        }

        @NotNull
        @Override
        public String[] getNames(Project project, boolean includeNonProjectItems) {
            return new String[0];
        }

        @NotNull
        @Override
        public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
            return new NavigationItem[0];
        }
    }

    class MyGotoCallback extends GotoActionBase.GotoActionCallback<FileType> {

        @Override
        public void elementChosen(ChooseByNamePopup popup, Object element) {
            if(element instanceof NavigationItem) {
                ((NavigationItem) element).navigate(true);
            }
        }

    }

}

