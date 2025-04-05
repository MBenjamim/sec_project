// SPDX-License-Identifier: MIT
pragma solidity ^0.8.22;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {IAccessControl} from "./AccessControl.sol";

contract ISTCoin is ERC20 {

    IAccessControl public accessControl;

    constructor(address recipient, address accessControlAddress) ERC20("IST Coin", "IST") {
        _mint(recipient, 100_000_000 * 10 ** decimals());
        accessControl = IAccessControl(accessControlAddress);
    }

    function decimals() public pure override returns (uint8) {
        return 2;
    }

    function transfer(address to, uint256 value) public override returns (bool) {
        require(!accessControl.isBlacklisted(msg.sender), "Sender is blacklisted");
        return super.transfer(to, value);
    }

    function transferFrom(address from, address to, uint256 value) public override returns (bool) {
        require(!accessControl.isBlacklisted(msg.sender), "Sender is blacklisted");
        return super.transferFrom(from, to, value);
    }
}
