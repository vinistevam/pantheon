description: Pantheon networking is about P2P discovery and communication between peers and access to the JSON RPC APIs
<!--- END of page meta data -->

# Networking

Pantheon uses the network to find and connect to peers. 

## Firewalls and Incoming Connections

The default logging configuration does not list node connection and disconnection messages.  

To enable listing of node connection and disconnection messages, specify the [`--logging`](../Reference/Pantheon-CLI-Syntax.md#logging) command line option `--logging=DEBUG`. For more verbosity, specify `--logging=TRACE`.  

The console logs connection and disconnection events when the log level is `DEBUG` or higher.  If `Successfully accepted connection from ...` is displayed, connections are getting through the firewalls. For example:

!!! example "Example log output"
    `2018-10-16 12:37:35.479-04:00 | nioEventLoopGroup-3-1 | INFO  | NettyP2PNetwork | Successfully accepted connection from 0xa979fb575495b8d6db44f750317d0f4622bf4c2aa3365d6af7c284339968eef29b69ad0dce72a4d8db5ebb4968de0e3bec910127f134779fbcb0cb6d3331163c`

If connections are not getting through the firewalls, ensure the peer discovery port is open on your firewall. 

## Peer Discovery Port

The [`--p2p-host`](../Reference/Pantheon-CLI-Syntax.md#p2p-host) and [`--p2p-port`](../Reference/Pantheon-CLI-Syntax.md#p2p-port)
options specifies the host and port on which P2P peer discovery listens. The default is ==127.0.0.1==
for host and ==30303== for port.
 
## Limiting Peers

Limiting peers reduces the bandwidth used by Pantheon. It also reduces the CPU time and disk access used to manage and respond to peers.  
 
Use the [`--max-peers`](../Reference/Pantheon-CLI-Syntax.md#max-peers) command line option to reduce the maximum number of peers. The default is 25.
 
Use the  [`--max-trailing-peers`](../Reference/Pantheon-CLI-Syntax.md#max-trailing-peers) option to reduce the maximum P2P peer connections for peers that are trailing behind the local chain head. The default is unlimited but the number of trailing peers cannot exceed the value specified by [`--max-peers`](../Reference/Pantheon-CLI-Syntax.md#max-peers).

Trailing peers cannot be used to get new blocks and are more likely to be requesting blocks from you. Limiting trailing peers may reduce the time taken to catch up to the chain head when synchronizing. 

## No Discovery

The [`--no-discovery`](../Reference/Pantheon-CLI-Syntax.md#no-discovery) command line option disables P2P peer discovery.  Only use this option if you are running a test node or a test network with fixed nodes.

## Monitoring Peer Connections

Use the [`debug_metrics`](../Reference/JSON-RPC-API-Methods.md#debug_metrics) JSON-RPC API method to obtain information about peer connections.   