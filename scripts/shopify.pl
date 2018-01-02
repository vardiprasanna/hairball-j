#!/usr/bin/perl
use LWP::UserAgent;

my $total = $#ARGV + 1;
if ($total < 1) {
    print "usage: ./shopify.pl [file]\neg: ./shopify.pl advertisers.txt\n";
    exit;
}

my $filename=$ARGV[0];
my $ftmp='tmp.' . `date "+%m%d%y-%H%M%S"`;

open( my $fh, '<', $filename ) or die "Can't open $filename: $!";
my %url_visited = ();
my @url_found = ();

my $ua = LWP::UserAgent->new;
$ua->ssl_opts(verify_hostname => 0);
$ua->timeout(5);
$| = 1;
$total = 0;
 
while ( my $line = <$fh> ) {
  if ( $line =~ /"id":([0-9]+).*"url":"([^\"]*)"/ ) {
    my $id = $1;
    my $url = lc $2;

    if ($url !~ /(http:|https:).*$/) {
        $url = 'http://'.$url;
    }
    if ($url !~ /^.*\/$/) {
      $url = $url.'/';
    }
    if ($url_visited{$url}) {
      # skip a visited url
      $url_visited{$url} = $url_visited{$url} . ',' . $id;
      next;
    }

    $url_visited{$url} = $id;
    $total=$total + 1;

    `curl -L -G -m5 -w "HTTPSTATUS:%{http_code}" $url 2> /dev/null > $ftmp`;
    my $httpStatus = `egrep 'HTTPSTATUS:[0-9]+' $ftmp | cut -d: -f2`;

    if ($httpStatus >= 200 && $httpStatus < 300) {
      my $hasShopify = `egrep -i -c -m1 "ShopifyAnalytics" $ftmp`;
      if ($hasShopify == 1) {
        print STDERR "$total - ", $url, " (yes)\n";
        push @url_found, $url;
        next;
      }
    }

    print STDERR "$total - ", $url, " (no)\n";
  }
}

close $fh;

# print the crawling result
foreach (@url_found) {
  print "found shopify client: '$_': $url_visited{$_}\n";
}

=for comment
foreach (sort keys %url_visited) {
  print "visited: '$_', $url_visited{$_}\n";
}
=cut
