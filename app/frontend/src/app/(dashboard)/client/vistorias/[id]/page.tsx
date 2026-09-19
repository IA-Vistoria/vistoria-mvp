import { InspectionWorkflow } from "@/features/inspections/client/inspection-workflow";

export default async function InspectionPage({ params }: PageProps<"/client/vistorias/[id]">) {
  const { id } = await params;
  return <InspectionWorkflow inspectionId={Number(id)} />;
}

