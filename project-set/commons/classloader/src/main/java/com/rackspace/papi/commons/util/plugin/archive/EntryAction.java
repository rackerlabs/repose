package com.rackspace.papi.commons.util.plugin.archive;

public class EntryAction {

   public static final EntryAction SKIP = new EntryAction();
   private final ProcessingAction processingAction;
   private final DeploymentAction packagingAction;

   private EntryAction() {
      this(ProcessingAction.SKIP, DeploymentAction.defaultAction());
   }

   public EntryAction(ProcessingAction processingAction, DeploymentAction packagingAction) {
      this.processingAction = processingAction;
      this.packagingAction = packagingAction;
   }

   public DeploymentAction deploymentAction() {
      return packagingAction;
   }

   public ProcessingAction processingAction() {
      return processingAction;
   }
}
