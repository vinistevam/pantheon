## Start Pantheon with Privacy Enabled

To start a Pantheon node with private transaction capabilities, one must
first start an Enclave. Currently, only the Orion enclave is supported.
This document outlines how to start a simple private network on a local
machine and how to send and verify Private Transactions between these
nodes.

!!!note
    There is currently no client to easily create and send transaction with.
    This document will use 3 pre-built signed RLP encoded transactions.

## Prerequisites 

[Pantheon](../Installation/Install-Binaries.md) 

[Orion](https://docs.orion.pegasys.tech/en/latest/Installation/Install-Binaries/)

[Curl (or similar web service client)](https://curl.haxx.se/download.html) 

### Enclave (Orion)

Below is an example of the minimum configuration to set-up an Orion node 
that will interact with Pantheon in the processing of Private Transactions.
Orion can be started independently from Pantheon.

Please consult the [Orion CLI documentation](https://docs.orion.pegasys.tech/en/latest/Reference/Orion-CLI-Syntax/#configuration-file) 
and [Orion Configuration documentation](https://docs.orion.pegasys.tech/en/latest/Configuring-Orion/Configuration-File/) 
for further details.

```bash tab="Syntax"
orion <FILE>
```

```bash tab="Example Command Line"
orion /home/me/me_enclave/orion.conf
```

```bash tab="Example Configuration File"
nodeurl = "http://127.0.0.1:8080/"
nodeport = 8080
nodenetworkinterface = "127.0.0.1"
clienturl = "http://127.0.0.1:8888/"
clientport = 8888
clientnetworkinterface = "127.0.0.1"
workdir = "data"
othernodes = ["http://127.0.0.1:8081/"]
publickeys = ["key.pub"]
privatekeys = ["key.key"]
```

!!!note
    You must have an Orion Enclave running with the `nodeurl` set to `http://127.0.0.1:8081/`
    and `nodeport` set to `8081` for the above configuration to work.
    
!!!note
    You do not need to specify `othernodes` for the first Orion instance that is created.

### Starting Pantheon

Below is an example of the minimum configuration to set-up a Pantheon node 
capable of sending and processing Private Transactions.

```bash tab="Syntax"
pantheon --network=<NETWORK> --rpc-http-api=<api name>[,<api name>...]... --privacy-enabled[=<true|false>] --privacy-url=<url> --privacy-public-key-file=<FILE>
```

```bash tab="Example Command Line"
pantheon --network=dev --rpc-http-api=EEA --privacy-enabled --privacy-url=http://meenclave.com/8888 --privacy-public-key-file=/home/me/me_enclave/data/key
```

```bash tab="Example Configuration File"
network="dev" 
rpc-http-api="EEA" 
privacy-enabled=true 
privacy-url="http://meenclave.com/8888"
privacy-public-key-file="/home/me/me_enclave/data/key"
```

More information about the option used above can be found via the following links:
- [network](../Reference/Pantheon-CLI-Syntax.md#network)
- [rpc-http-api](../Reference/Pantheon-CLI-Syntax.md#rpc-http-api)
- [privacy-enabled](../Reference/Pantheon-CLI-Syntax.md#privacy-enabled)
- [privacy-url](../Reference/Pantheon-CLI-Syntax.md#privacy-url)
- [privacy-public-key-file](../Reference/Pantheon-CLI-Syntax.md#privacy-public-key-file)

!!!note
    Privacy is under development and will be available in v1.1.
    
## Private Transaction walkthrough

To create a 3-node private network on your local machine: 

1. [Create Folders](#1-create-folders)
2. [Create Genesis File](#2-create-genesis-file)
3. [Start instances of Orion for each node](#3-start-instances-of-orion-for-each-node)
4. [Get Public Key of First Node](#4-get-public-key-of-first-node)
5. [Start First Node as Bootnode](#5-restart-first-node-as-bootnode)
6. [Start Node-2](#6-start-node-2)
7. [Start Node-3-non-privacy](#7-start-node-3)
8. [Confirm the private network is working](#8-confirm-private-network-is-working)
9. [Create a Private Transaction between Node-1 with Node-2](#9-create-a-private-transaction-node-1-with-node-2)
10. [Confirm Node-3 can't interact with private Transaction](#10-confirm-node-3-can't-interact-with-private-transaction)


### 1. Create Folders 

Each node requires a data directory for the blockchain data. When the
node is started, the node key is saved in this directory.

Create directories for your private network, each of the three nodes, and a data directory for each node: 

```bash
Private-Network/
├── Node-1
│   ├── Node-1-data-path
├── Node-2
│   ├── Node-2-data-path
└── Node-3-non-privacy
    ├── Node-3-data-path
```

### 2. Create Genesis File 

The genesis file defines the genesis block of the blockchain (that is, the initial state of the blockchain).
The genesis file includes entries for configuring the blockchain such as the mining difficulty and initial 
accounts and balances.    

All nodes in a network must use the same genesis file. The [network ID](../Configuring-Pantheon/NetworkID-And-ChainID.md) 
defaults to the `chainID` in the genesis file. The `fixeddifficulty` enables blocks to be mined quickly.   

Copy the following genesis definition to a file called `privateNetworkGenesis.json` and save it in the `Private-Network` directory: 

```json
{
  "config": {
      "constantinoplefixblock": 0,
      "ethash": {
        "fixeddifficulty": 1000
      },
       "chainID": 1981
   },
  "nonce": "0x42",
  "gasLimit": "0x1000000",
  "difficulty": "0x10000",
  "alloc": {
    "fe3b557e8fb62b89f4916b721be55ceb828dbd73": {
      "privateKey": "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
      "comment": "private key and this comment are ignored.  In a real chain, the private key should NOT be stored",
      "balance": "0xad78ebc5ac6200000"
    },
    "f17f52151EbEF6C7334FAD080c5704D77216b732": {
      "privateKey": "ae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f",
      "comment": "private key and this comment are ignored.  In a real chain, the private key should NOT be stored",
      "balance": "90000000000000000000000"
    }
  }
}
```

!!! warning
    Do not use the accounts in the genesis file above on mainnet or any public network except for testing.
    The private keys are displayed so the accounts are not secure. 
    
### 3. Start instances of Orion for each node

Download and install [*Orion*](https://github.com/PegaSysEng/orion/blob/master/documentation/install/running.md) 
to be used as an enclave to store and communicate the private transactions in Pantheon.

We can generate key pairs for Orion to use using the following command `orion -f foo`. This will generate
a public-private key pair which will be used to connect to Orion instance. The public key generated  
link the Pantheon node to Orion instance.

Refer to [Configuring Orion](https://github.com/PegaSysEng/orion/blob/master/documentation/install/configure.md)
for a detailed configuration options. 

Start one instance of Orion for each Pantheon node which we intend to perform private transactions using
`orion foo.conf`

    
### 4. Get Public Key of First Node

To enable nodes to discover each other, a network requires one or more nodes to be bootnodes. 
For this private network, we will use Node-1 as the bootnode. This requires obtaining the public key for the [enode URL](../Configuring-Pantheon/Node-Keys.md#enode-url). 

In the `Node-1` directory, use the [`public-key` subcommand](../Reference/Pantheon-CLI-Syntax.md#public-key) to write 
the [node public key](../Configuring-Pantheon/Node-Keys.md#node-public-key) to the specified file (`publicKeyNode1` in this example):

```bash tab="MacOS"
pantheon --data-path=Node-1-data-path --genesis-file=../privateNetworkGenesis.json public-key export --to=Node-1-data-path/publicKeyNode1
```

```bash tab="Windows"
pantheon --data-path=Node-1-data-path --genesis-file=..\privateNetworkGenesis.json public-key export --to=Node-1-data-path\publicKeyNode1
```

!!!note
    The [`--data-path`](../Reference/Pantheon-CLI-Syntax.md#data-path) and [`--genesis-file`](../Reference/Pantheon-CLI-Syntax.md#genesis-file) 
    options are not used when running Pantheon from the [Docker image](../Getting-Started/Run-Docker-Image.md). 
    Use a bind mount to [specify a configuration file with Docker](../Getting-Started/Run-Docker-Image.md#custom-genesis-file)
    and volume to [specify the data directory](../Getting-Started/Run-Docker-Image.md#data-directory).

Your node 1 directory now contains: 
```bash
├── Node-1
    ├── Node-1-data-path
        ├── database
        ├── key
        ├── publicKeyNode1
```
      
The `database` directory contains the blockchain data. 

### 5. Start First Node as Bootnode 

Start Node-1:

```bash tab="MacOS"
pantheon --data-path=Node-1-data-path --genesis-file=../privateNetworkGenesis.json --bootnodes 
--miner-enabled --miner-coinbase fe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-enabled 
--host-whitelist=* --rpc-http-cors-origins="all" --privacy-enabled=true --privacy-precompiled-address=125
--privacy-url=127.0.0.1:8888 --privacy-public-key-file=../pathToOrion1PublicKey.pub --rpc-http-api=EEA    
```

```bash tab="Windows"
pantheon --data-path=Node-1-data-path --genesis-file=..\privateNetworkGenesis.json --bootnodes
 --miner-enabled --miner-coinbase fe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-enabled
 --host-whitelist=* --rpc-http-cors-origins="all" --privacy-enabled=true --privacy-precompiled-address=125
 --privacy-url=127.0.0.1:8888 --privacy-public-key-file=..\pathToOrion1PublicKey.pub --rpc-http-api=EEA   
```

The command line specifies: 

* No arguments for the [`--bootnodes`](../Reference/Pantheon-CLI-Syntax.md#bootnodes) option because this is your bootnode.
* Mining is enabled and the account to which mining rewards are paid using the [`--miner-enabled`](../Reference/Pantheon-CLI-Syntax.md#miner-enabled) 
and [`--miner-coinbase`](../Reference/Pantheon-CLI-Syntax.md#miner-coinbase) options.
* JSON-RPC API is enabled using the [`--rpc-http-enabled`](../Reference/Pantheon-CLI-Syntax.md#rpc-http-enabled) option.
* All hosts can access the HTTP JSON-RPC API using the [`--host-whitelist`](../Reference/Pantheon-CLI-Syntax.md#host-whitelist) option.
* All domains can access the node using the HTTP JSON-RPC API using the [`--rpc-http-cors-origins`](../Reference/Pantheon-CLI-Syntax.md#rpc-http-cors-origins) option.  
* [`--privacy-enabled=true`](../Reference/Pantheon-CLI-Syntax.md#privacy-enabled)
  set true to enable the privacy.
* Changes the default Privacy PreCompiled address
  [`--privacy-precompiled-address=125`](../Reference/Pantheon-CLI-Syntax.md#privacy-precompiled-address)
* Setup the enclave(orion) URL
  [`--privacy-url=127.0.0.1:8888`](../Reference/Pantheon-CLI-Syntax.md#privacy-url)
* Pass the enclave(orion) public Key
  [`--privacy-public-key-file`](../Reference/Pantheon-CLI-Syntax.md#privacy-public-key-file)
* Enable EEA methods
  [`--rpc-http-api=EEA`](../Reference/Pantheon-CLI-Syntax.md#rpc-http-api)

!!! info The miner coinbase account is one of the accounts defined in
the genesis file. 

!!! info The Privacy PreCompiled address need to be
the same address for each node interacting through the private
transaction.

### 6. Start Node-2 

You need the [enode URL](../Configuring-Pantheon/Node-Keys.md#enode-url) for Node-1 to specify Node-1 as the bootnode for Node-2 and Node-3. 

Start another terminal, change to the `Node-2` directory and start Node-2 replacing the enode URL with your bootnode:

```bash tab="MacOS"
pantheon --data-path=Node-2-data-path --genesis-file=../privateNetworkGenesis.json 
--bootnodes="enode://<node public key ex 0x>@127.0.0.1:30303" --p2p-port=30304  
 --privacy-enabled=true --privacy-precompiled-address=125
 --privacy-url=127.0.0.1:8888 --privacy-public-key-file=../pathToOrion2PublicKey.pub --rpc-http-api=EEA  
```

```bash tab="Windows"
pantheon --data-path=Node-2-data-path --genesis-file=..\privateNetworkGenesis.json --bootnodes="enode://<node public key ex 0x>@127.0.0.1:30303" --p2p-port=30304
--privacy-enabled=true --privacy-precompiled-address=125 --privacy-url=127.0.0.1:8888 --privacy-public-key-file=..\pathToOrion2PublicKey.pub --rpc-http-api=EEA      
```

The command line specifies: 

* Different port to Node-1 for P2P peer discovery using the [`--p2p-port`](../Reference/Pantheon-CLI-Syntax.md#p2p-port) option.
* Enode URL for Node-1 using the [`--bootnodes`](../Reference/Pantheon-CLI-Syntax.md#bootnodes) option.
* Data directory for Node-2 using the [`--data-path`](../Reference/Pantheon-CLI-Syntax.md#data-path) option.
* Genesis file as for Node-1.confirm-private-network-is-working
* [`--privacy-enabled=true`](../Reference/Pantheon-CLI-Syntax.md#privacy-enabled)
  set true to enable privacy.
* Changes the default Privacy PreCompiled address
  [`--privacy-precompiled-address=125`](../Reference/Pantheon-CLI-Syntax.md#privacy-precompiled-address)
* Setup the enclave(orion) URL
  [`--privacy-url=127.0.0.1:8888`](../Reference/Pantheon-CLI-Syntax.md#privacy-url)
* Enable EEA methods
  [`--rpc-http-api=EEA`](../Reference/Pantheon-CLI-Syntax.md#rpc-http-api)  

### 7. Start Node-3

Start another terminal, change to the `Node-3` directory and start Node-3 replacing the enode URL with your bootnode: 

```bash tab="MacOS"
pantheon --data-path=Node-3-data-path --genesis-file=../privateNetworkGenesis.json 
--bootnodes="enode://<node public key ex 0x>@127.0.0.1:30303" --p2p-port30305
```

```bash tab="Windows"
pantheon --data-path=Node-3-data-path --genesis-file=..\privateNetworkGenesis.json 
--bootnodes="enode://<node public key ex 0x>@127.0.0.1:30303" --p2p-port=30305    
```

The command line specifies: 

 * Different port to Node-1 and Node-2 for P2P peer discovery.
 * Data directory for Node-3 using the [`--data-path`](../Reference/Pantheon-CLI-Syntax.md#data-path) option.
 * Bootnode and genesis file as for Node-2. 
*  Without privacy commandline.

### 8. Confirm Private Network is Working 

Start another terminal, use curl to call the JSON-RPC API [`net_peerCount`](../Reference/JSON-RPC-API-Methods.md#net_peercount) method and confirm the nodes are functioning as peers: 

```bash
curl -X POST --data '{"jsonrpc":"2.0","method":"net_peerCount","params":[],"id":1}' localhost:8545
```

The result confirms Node-1 (the node running the JSON-RPC service) has two peers (Node-2 and Node-3):
```json
{
  "jsonrpc" : "2.0",
  "id" : 1,
  "result" : "0x2"
}
```
 

### 9. Create a Private Transaction between Node-1 with Node-2

### 10. Confirm Node-3 can't interact with private Transaction



!!!note EEA methods are for privacy features. Privacy features are under development and will be available in v1.1.
