import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type WhatsAppBlockProps = {
  /** Only non-null when the server has confirmed visibility + membership rules. */
  whatsappUrl: string | null;
  whatsappVisibleToMembers: boolean;
};

/**
 * Traveler-facing WhatsApp CTA — receives already-masked data from the server
 * (via RPC) so the real URL is never sent when hidden.
 */
export function WhatsAppBlock({ whatsappUrl, whatsappVisibleToMembers }: WhatsAppBlockProps) {
  const showButton = whatsappVisibleToMembers && !!whatsappUrl;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Trip group chat</CardTitle>
        <CardDescription>We coordinate on WhatsApp for this trip.</CardDescription>
      </CardHeader>
      <CardContent>
        {showButton ? (
          <Button asChild>
            <Link href={whatsappUrl!} target="_blank" rel="noopener noreferrer">
              Join WhatsApp group
            </Link>
          </Button>
        ) : (
          <p className="text-sm text-muted-foreground">Your guide will share the WhatsApp group link when ready.</p>
        )}
      </CardContent>
    </Card>
  );
}
