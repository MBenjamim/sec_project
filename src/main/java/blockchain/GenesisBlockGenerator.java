package main.java.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import main.java.crypto_utils.RSAKeyReader;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.internal.Words;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.*;

public class GenesisBlockGenerator {
    private static final Logger logger = LoggerFactory.getLogger(GenesisBlockGenerator.class);

    private static final String publicKeysDir = "public_keys/";
    private static final String genesisBlockPath = "genesis_block.json";

    /**
     * AccessControl.sol bytecode from data->bytecode->object since they will be deployed using EVM
     * Available functions:
     *  - addToBlacklist(address)
     *  - authorizedParties(address)
     *  - isBlacklisted(address)
     *  - owner()
     *  - removeFromBlacklist(address)
     *  - setAuthorizedParty(address,bool)
     */
    private static final Bytes accessControlBytecode = Bytes.fromHexString("6080604052348015600e575f80fd5b50335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550600160025f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff021916908315150217905550610a3a806100d05f395ff3fe608060405234801561000f575f80fd5b5060043610610060575f3560e01c806344337ea114610064578063537df3b6146100945780638da5cb5b146100c4578063b8d2e1db146100e2578063c7822dc414610112578063fe575a871461012e575b5f80fd5b61007e600480360381019061007991906106fe565b61015e565b60405161008b9190610743565b60405180910390f35b6100ae60048036038101906100a991906106fe565b610344565b6040516100bb9190610743565b60405180910390f35b6100cc610529565b6040516100d9919061076b565b60405180910390f35b6100fc60048036038101906100f791906106fe565b61054c565b6040516101099190610743565b60405180910390f35b61012c600480360381019061012791906107ae565b610569565b005b610148600480360381019061014391906106fe565b61064e565b6040516101559190610743565b60405180910390f35b5f6001151560025f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff161515146101ef576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016101e690610846565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff160361025d576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610254906108ae565b60405180910390fd5b60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff16156102e7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016102de90610916565b60405180910390fd5b6001805f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff02191690831515021790555060019050919050565b5f6001151560025f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff161515146103d5576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103cc90610846565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603610443576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161043a906108ae565b60405180910390fd5b60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff166104cc576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104c39061097e565b60405180910390fd5b5f60015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff02191690831515021790555060019050919050565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b6002602052805f5260405f205f915054906101000a900460ff1681565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16146105f6576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105ed906109e6565b60405180910390fd5b8060025f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055505050565b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6106cd826106a4565b9050919050565b6106dd816106c3565b81146106e7575f80fd5b50565b5f813590506106f8816106d4565b92915050565b5f60208284031215610713576107126106a0565b5b5f610720848285016106ea565b91505092915050565b5f8115159050919050565b61073d81610729565b82525050565b5f6020820190506107565f830184610734565b92915050565b610765816106c3565b82525050565b5f60208201905061077e5f83018461075c565b92915050565b61078d81610729565b8114610797575f80fd5b50565b5f813590506107a881610784565b92915050565b5f80604083850312156107c4576107c36106a0565b5b5f6107d1858286016106ea565b92505060206107e28582860161079a565b9150509250929050565b5f82825260208201905092915050565b7f4e6f7420617574686f72697a65640000000000000000000000000000000000005f82015250565b5f610830600e836107ec565b915061083b826107fc565b602082019050919050565b5f6020820190508181035f83015261085d81610824565b9050919050565b7f496e76616c6964206164647265737300000000000000000000000000000000005f82015250565b5f610898600f836107ec565b91506108a382610864565b602082019050919050565b5f6020820190508181035f8301526108c58161088c565b9050919050565b7f416c726561647920626c61636b6c6973746564000000000000000000000000005f82015250565b5f6109006013836107ec565b915061090b826108cc565b602082019050919050565b5f6020820190508181035f83015261092d816108f4565b9050919050565b7f4e6f7420626c61636b6c697374656400000000000000000000000000000000005f82015250565b5f610968600f836107ec565b915061097382610934565b602082019050919050565b5f6020820190508181035f8301526109958161095c565b9050919050565b7f4e6f7420617574686f72697a656420286f6e6c79206f776e65722900000000005f82015250565b5f6109d0601b836107ec565b91506109db8261099c565b602082019050919050565b5f6020820190508181035f8301526109fd816109c4565b905091905056fea2646970667358221220e641fe914213c54e66bb8ca2a5866be58954d98549563ce87fe57297eaf3283c64736f6c634300081a0033");


    /**
     * ISTCoin.sol bytecode from data->bytecode->object since they will be deployed using EVM
     * Available functions:
     *  - accessControl()
     *  - allowance(address,address)
     *  - approve(address,uint256)
     *  - balanceOf(address)
     *  - decimals()
     *  - name()
     *  - symbol()
     *  - totalSupply()
     *  - transfer(address,uint256)
     *  - transferFrom(address,address,uint256)
     */
    private static final Bytes fungibleTokenBytecode = Bytes.fromHexString("608060405234801561000f575f80fd5b50604051611b68380380611b6883398181016040528101906100319190610441565b6040518060400160405280600881526020017f49535420436f696e0000000000000000000000000000000000000000000000008152506040518060400160405280600381526020017f495354000000000000000000000000000000000000000000000000000000000081525081600390816100ac91906106a6565b5080600490816100bc91906106a6565b5050506100f7336100d161013d60201b60201c565b600a6100dd91906108dd565b6305f5e1006100ec9190610927565b61014560201b60201c565b8060055f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050610a20565b5f6002905090565b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036101b5575f6040517fec442f050000000000000000000000000000000000000000000000000000000081526004016101ac9190610977565b60405180910390fd5b6101c65f83836101ca60201b60201c565b5050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff160361021a578060025f82825461020e9190610990565b925050819055506102e8565b5f805f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20549050818110156102a3578381836040517fe450d38c00000000000000000000000000000000000000000000000000000000815260040161029a939291906109d2565b60405180910390fd5b8181035f808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2081905550505b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff160361032f578060025f8282540392505081905550610379565b805f808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f82825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef836040516103d69190610a07565b60405180910390a3505050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610410826103e7565b9050919050565b61042081610406565b811461042a575f80fd5b50565b5f8151905061043b81610417565b92915050565b5f60208284031215610456576104556103e3565b5b5f6104638482850161042d565b91505092915050565b5f81519050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f60028204905060018216806104e757607f821691505b6020821081036104fa576104f96104a3565b5b50919050565b5f819050815f5260205f209050919050565b5f6020601f8301049050919050565b5f82821b905092915050565b5f6008830261055c7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff82610521565b6105668683610521565b95508019841693508086168417925050509392505050565b5f819050919050565b5f819050919050565b5f6105aa6105a56105a08461057e565b610587565b61057e565b9050919050565b5f819050919050565b6105c383610590565b6105d76105cf826105b1565b84845461052d565b825550505050565b5f90565b6105eb6105df565b6105f68184846105ba565b505050565b5b818110156106195761060e5f826105e3565b6001810190506105fc565b5050565b601f82111561065e5761062f81610500565b61063884610512565b81016020851015610647578190505b61065b61065385610512565b8301826105fb565b50505b505050565b5f82821c905092915050565b5f61067e5f1984600802610663565b1980831691505092915050565b5f610696838361066f565b9150826002028217905092915050565b6106af8261046c565b67ffffffffffffffff8111156106c8576106c7610476565b5b6106d282546104d0565b6106dd82828561061d565b5f60209050601f83116001811461070e575f84156106fc578287015190505b610706858261068b565b86555061076d565b601f19841661071c86610500565b5f5b828110156107435784890151825560018201915060208501945060208101905061071e565b86831015610760578489015161075c601f89168261066f565b8355505b6001600288020188555050505b505050505050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f8160011c9050919050565b5f808291508390505b60018511156107f7578086048111156107d3576107d2610775565b5b60018516156107e25780820291505b80810290506107f0856107a2565b94506107b7565b94509492505050565b5f8261080f57600190506108ca565b8161081c575f90506108ca565b8160018114610832576002811461083c5761086b565b60019150506108ca565b60ff84111561084e5761084d610775565b5b8360020a91508482111561086557610864610775565b5b506108ca565b5060208310610133831016604e8410600b84101617156108a05782820a90508381111561089b5761089a610775565b5b6108ca565b6108ad84848460016107ae565b925090508184048111156108c4576108c3610775565b5b81810290505b9392505050565b5f60ff82169050919050565b5f6108e78261057e565b91506108f2836108d1565b925061091f7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8484610800565b905092915050565b5f6109318261057e565b915061093c8361057e565b925082820261094a8161057e565b9150828204841483151761096157610960610775565b5b5092915050565b61097181610406565b82525050565b5f60208201905061098a5f830184610968565b92915050565b5f61099a8261057e565b91506109a58361057e565b92508282019050808211156109bd576109bc610775565b5b92915050565b6109cc8161057e565b82525050565b5f6060820190506109e55f830186610968565b6109f260208301856109c3565b6109ff60408301846109c3565b949350505050565b5f602082019050610a1a5f8301846109c3565b92915050565b61113b80610a2d5f395ff3fe608060405234801561000f575f80fd5b506004361061009c575f3560e01c8063313ce56711610064578063313ce5671461015a57806370a082311461017857806395d89b41146101a8578063a9059cbb146101c6578063dd62ed3e146101f65761009c565b806306fdde03146100a0578063095ea7b3146100be57806313007d55146100ee57806318160ddd1461010c57806323b872dd1461012a575b5f80fd5b6100a8610226565b6040516100b59190610c83565b60405180910390f35b6100d860048036038101906100d39190610d34565b6102b6565b6040516100e59190610d8c565b60405180910390f35b6100f66102d8565b6040516101039190610e00565b60405180910390f35b6101146102fd565b6040516101219190610e28565b60405180910390f35b610144600480360381019061013f9190610e41565b610306565b6040516101519190610d8c565b60405180910390f35b6101626103f4565b60405161016f9190610eac565b60405180910390f35b610192600480360381019061018d9190610ec5565b6103fc565b60405161019f9190610e28565b60405180910390f35b6101b0610441565b6040516101bd9190610c83565b60405180910390f35b6101e060048036038101906101db9190610d34565b6104d1565b6040516101ed9190610d8c565b60405180910390f35b610210600480360381019061020b9190610ef0565b6105bd565b60405161021d9190610e28565b60405180910390f35b60606003805461023590610f5b565b80601f016020809104026020016040519081016040528092919081815260200182805461026190610f5b565b80156102ac5780601f10610283576101008083540402835291602001916102ac565b820191905f5260205f20905b81548152906001019060200180831161028f57829003601f168201915b5050505050905090565b5f806102c061063f565b90506102cd818585610646565b600191505092915050565b60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b5f600254905090565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663fe575a87336040518263ffffffff1660e01b81526004016103619190610f9a565b602060405180830381865afa15801561037c573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906103a09190610fdd565b156103e0576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103d790611052565b60405180910390fd5b6103eb848484610658565b90509392505050565b5f6002905090565b5f805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20549050919050565b60606004805461045090610f5b565b80601f016020809104026020016040519081016040528092919081815260200182805461047c90610f5b565b80156104c75780601f1061049e576101008083540402835291602001916104c7565b820191905f5260205f20905b8154815290600101906020018083116104aa57829003601f168201915b5050505050905090565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663fe575a87336040518263ffffffff1660e01b815260040161052c9190610f9a565b602060405180830381865afa158015610547573d5f803e3d5ffd5b505050506040513d601f19601f8201168201806040525081019061056b9190610fdd565b156105ab576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105a290611052565b60405180910390fd5b6105b58383610686565b905092915050565b5f60015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054905092915050565b5f33905090565b61065383838360016106a8565b505050565b5f8061066261063f565b905061066f858285610877565b61067a85858561090a565b60019150509392505050565b5f8061069061063f565b905061069d81858561090a565b600191505092915050565b5f73ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff1603610718575f6040517fe602df0500000000000000000000000000000000000000000000000000000000815260040161070f9190610f9a565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610788575f6040517f94280d6200000000000000000000000000000000000000000000000000000000815260040161077f9190610f9a565b60405180910390fd5b8160015f8673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20819055508015610871578273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925846040516108689190610e28565b60405180910390a35b50505050565b5f61088284846105bd565b90507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff81101561090457818110156108f5578281836040517ffb8f41b20000000000000000000000000000000000000000000000000000000081526004016108ec93929190611070565b60405180910390fd5b61090384848484035f6106a8565b5b50505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff160361097a575f6040517f96c6fd1e0000000000000000000000000000000000000000000000000000000081526004016109719190610f9a565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036109ea575f6040517fec442f050000000000000000000000000000000000000000000000000000000081526004016109e19190610f9a565b60405180910390fd5b6109f58383836109fa565b505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610a4a578060025f828254610a3e91906110d2565b92505081905550610b18565b5f805f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054905081811015610ad3578381836040517fe450d38c000000000000000000000000000000000000000000000000000000008152600401610aca93929190611070565b60405180910390fd5b8181035f808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2081905550505b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603610b5f578060025f8282540392505081905550610ba9565b805f808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f82825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef83604051610c069190610e28565b60405180910390a3505050565b5f81519050919050565b5f82825260208201905092915050565b8281835e5f83830152505050565b5f601f19601f8301169050919050565b5f610c5582610c13565b610c5f8185610c1d565b9350610c6f818560208601610c2d565b610c7881610c3b565b840191505092915050565b5f6020820190508181035f830152610c9b8184610c4b565b905092915050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610cd082610ca7565b9050919050565b610ce081610cc6565b8114610cea575f80fd5b50565b5f81359050610cfb81610cd7565b92915050565b5f819050919050565b610d1381610d01565b8114610d1d575f80fd5b50565b5f81359050610d2e81610d0a565b92915050565b5f8060408385031215610d4a57610d49610ca3565b5b5f610d5785828601610ced565b9250506020610d6885828601610d20565b9150509250929050565b5f8115159050919050565b610d8681610d72565b82525050565b5f602082019050610d9f5f830184610d7d565b92915050565b5f819050919050565b5f610dc8610dc3610dbe84610ca7565b610da5565b610ca7565b9050919050565b5f610dd982610dae565b9050919050565b5f610dea82610dcf565b9050919050565b610dfa81610de0565b82525050565b5f602082019050610e135f830184610df1565b92915050565b610e2281610d01565b82525050565b5f602082019050610e3b5f830184610e19565b92915050565b5f805f60608486031215610e5857610e57610ca3565b5b5f610e6586828701610ced565b9350506020610e7686828701610ced565b9250506040610e8786828701610d20565b9150509250925092565b5f60ff82169050919050565b610ea681610e91565b82525050565b5f602082019050610ebf5f830184610e9d565b92915050565b5f60208284031215610eda57610ed9610ca3565b5b5f610ee784828501610ced565b91505092915050565b5f8060408385031215610f0657610f05610ca3565b5b5f610f1385828601610ced565b9250506020610f2485828601610ced565b9150509250929050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f6002820490506001821680610f7257607f821691505b602082108103610f8557610f84610f2e565b5b50919050565b610f9481610cc6565b82525050565b5f602082019050610fad5f830184610f8b565b92915050565b610fbc81610d72565b8114610fc6575f80fd5b50565b5f81519050610fd781610fb3565b92915050565b5f60208284031215610ff257610ff1610ca3565b5b5f610fff84828501610fc9565b91505092915050565b7f53656e64657220697320626c61636b6c697374656400000000000000000000005f82015250565b5f61103c601583610c1d565b915061104782611008565b602082019050919050565b5f6020820190508181035f83015261106981611030565b9050919050565b5f6060820190506110835f830186610f8b565b6110906020830185610e19565b61109d6040830184610e19565b949350505050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f6110dc82610d01565b91506110e783610d01565b92508282019050808211156110ff576110fe6110a5565b5b9291505056fea26469706673582212203634eb90a41e7a90c60962c9eda59318c6e4e630d7722beffdfb44385e43d2f864736f6c634300081a0033");

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            logger.error("Usage: GenesisBlockGenerator <number-of-users>");
        }

        int numUsers = Integer.parseInt(args[0]);
        List<Address> eoaList = generateClientAddrs(numUsers);
        if (eoaList == null) return;

        // set the world and EVM
        SimpleWorld world = new SimpleWorld();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        EVMExecutor executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
                .tracer(tracer)
                .worldUpdater(world.updater())
                .commitWorldState();

        // create client accounts (i.e. EOA accounts) from their addresses
        for (Address address : eoaList) {
            world.getOrCreate(address);
        }

        // assume client with id zero is the owner of both contracts
        Address ownerAddr = eoaList.get(0);

        // deploy AccessControl.sol
        Address accessControlAddr = deployContract(executor, accessControlBytecode, ownerAddr, 0);

        // deploy ISTCoin.sol using AccessControl.sol as AccessControl interface
        String paddedAccessControlAddr = padHexStringTo256Bit(accessControlAddr.toHexString());
        Bytes tokenBytecode = Bytes.fromHexString(fungibleTokenBytecode.toHexString() + paddedAccessControlAddr);
        Address tokenContractAddr = deployContract(executor, tokenBytecode, ownerAddr, 1);

        // Getting storage from AccessControl.sol
        MutableAccount deployedAccessContract = (MutableAccount) world.get(accessControlAddr);
        Bytes32 storedOwner = deployedAccessContract.getStorageValue(UInt256.valueOf(0));
        Bytes32 expectedOwner = Words.fromAddress(ownerAddr);

        String paddedAddress = padHexStringTo256Bit(ownerAddr.toHexString());
        String blacklistIndex = convertIntegerToHex256Bit(1);
        String blacklistKey = Numeric.toHexString(Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + blacklistIndex)));
        Bytes32 blacklistEntry = world.get(accessControlAddr).getStorageValue(UInt256.fromHexString(blacklistKey));

        String authorizedPartiesIndex = convertIntegerToHex256Bit(2);
        String authorizedPartiesKey = Numeric.toHexString(Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + authorizedPartiesIndex)));
        Bytes32 authorizedPartiesEntry = world.get(accessControlAddr).getStorageValue(UInt256.fromHexString(authorizedPartiesKey));

        // This is just debugging
        logger.info("Sender Address: {}", expectedOwner);
        logger.info("Stored Owner:   {}\tin slot {}", storedOwner, UInt256.valueOf(0).toHexString());
        logger.info("Do they match? {}", storedOwner.equals(expectedOwner));
        logger.info("Stored blacklist:  {}\tin slot {}", blacklistEntry, blacklistKey);
        logger.info("This value should be false -> value: {}", !blacklistEntry.equals(UInt256.valueOf(0)));
        logger.info("Stored authorized: {}\tin slot {}", authorizedPartiesEntry, authorizedPartiesKey);
        logger.info("This value should be true -> value: {}", !authorizedPartiesEntry.equals(UInt256.valueOf(0)));

        // for some reason balances are not updated directly by Transfer() event
        fixBalancesFromStorage(executor, world, tokenContractAddr, byteArrayOutputStream);

        Collection<MutableAccount> list = (Collection<MutableAccount>) world.getTouchedAccounts();
        for (MutableAccount account : list) {
            logger.info("{}\t{}\t{}", account.getAddress().toString(), world.getAccount(account.getAddress()).getBalance().toLong(), (account.getCode().equals(Bytes.fromHexString(""))) ? "EOA account" : "Contract Account");
        }

        // print storage of each contract - TODO: convert this fields to storage in genesis block
        logger.info("ACCESS CONTROL STORAGE");
        Map<UInt256, UInt256> storage1 = deployedAccessContract.getUpdatedStorage();
        for (Map.Entry<UInt256, UInt256> entry : storage1.entrySet()) {
            logger.info("{}\t{}", entry.getKey(), entry.getValue());
        }

        logger.info("IST COIN STORAGE");
        MutableAccount deployedTokenContract = (MutableAccount) world.get(tokenContractAddr);
        Map<UInt256, UInt256> storage2 = deployedTokenContract.getUpdatedStorage();
        for (Map.Entry<UInt256, UInt256> entry : storage2.entrySet()) {
            logger.info("{}\t{}", entry.getKey(), entry.getValue());
        }

        // Deprecated (TODO: create genesis block from world, also create a from json to recover world from blocks)
//        Block genesisBlock = new Block(null);
//
//        // this avoids generating address 0
//        for (int i = 0; i < numUsers; i++) {
//            Address address = eoaList.get(i);
//            genesisBlock.getState().put(address.toString(), new Account(address, 1000L));
//            logger.info("client{} has the address {}", i, address);
//        }
//
//        genesisBlock.hashBlock();
//        saveToFile(genesisBlock);
//        logger.info("Genesis block generated successfully!");
    }

    public static List<Address> generateClientAddrs(int numUsers) {
        List<Address> addresses = new ArrayList<>();
        for (int i = 0; i < numUsers; i++) {
            String publicKeyPath = getPublicKeyPath(i);
            PublicKey publicKey;
            try {
                publicKey = RSAKeyReader.readPublicKey(publicKeyPath);
                addresses.add(AddressGenerator.generateAddress(publicKey));
            } catch (Exception e) {
                logger.error("Error reading public key and generating address", e);
                return null;
            }
        }
        return addresses;
    }

    public static Address deployContract(EVMExecutor executor, Bytes bytecode, Address ownerAddr, long nonce) {
        Address contractAddr = Address.contractAddress(ownerAddr, nonce);
        executor.messageFrameType(MessageFrame.Type.CONTRACT_CREATION)
                .contract(contractAddr)
                .code(bytecode)
                .sender(ownerAddr)
                .receiver(contractAddr)
                .execute();
        return contractAddr;
    }

    public static void fixBalancesFromStorage(EVMExecutor executor, SimpleWorld world, Address tokenAddr, ByteArrayOutputStream out) {
        String functionSignature = getFunctionSignature("balanceOf(address)");
        Collection<MutableAccount> list = (Collection<MutableAccount>) world.getTouchedAccounts();
        for (MutableAccount account : list) {
            String paddedAddr = padHexStringTo256Bit(account.getAddress().toHexString());
            executor.messageFrameType(MessageFrame.Type.MESSAGE_CALL)
                    .code(world.getAccount(tokenAddr).getCode())
                    .callData(Bytes.fromHexString(functionSignature + paddedAddr))
                    .sender(tokenAddr)
                    .receiver(tokenAddr)
                    .execute();
            // update balance manually
            account.setBalance(Wei.fromHexString(Long.toHexString(extractIntegerFromReturnData(out))));
            executor.worldUpdater(world.updater()).commitWorldState();
        }
    }

    private static String getPublicKeyPath(int userId) {
        return publicKeysDir + "client" + userId + "_public.key";
    }

    private static void saveToFile(Block genesisBlock) throws IOException {
        // JSON without indentation
        String jsonString = genesisBlock.toJson();

        // Improve indentation
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        String indentedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

        try (FileOutputStream fos = new FileOutputStream(genesisBlockPath)) {
            fos.write(indentedJson.getBytes());
            logger.info("Saved genesis block:\n{}", indentedJson);
        }
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);

        return String.format("%064x", bigInt);
    }

    public static String stringToHex(String input) {
        StringBuilder hexString = new StringBuilder();
        for (char c : input.toCharArray()) {
            hexString.append(String.format("%02x", (int) c));
        }
        return hexString.toString();
    }

    public static String getFunctionSignature(String functionName) {
        byte[] functionBytes = Numeric.hexStringToByteArray(stringToHex(functionName));
        String functionHash = Numeric.toHexStringNoPrefix(Hash.sha3(functionBytes));
        return functionHash.substring(0, 8); // first 4 bytes of the hash
    }

    public static long extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Long.decode("0x"+returnData);
    }
}
