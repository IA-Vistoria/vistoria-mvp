import { EngineerReview } from "@/features/inspections/engineer/engineer-review";

export default async function EngineerReviewPage({ params }: PageProps<"/engineer/vistorias/[id]">) {
  const { id } = await params;
  return <EngineerReview inspectionId={Number(id)} />;
}
